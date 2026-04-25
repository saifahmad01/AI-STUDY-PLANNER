package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.ai.AiStudyPlanParser;
import com.studyplanner.backend.ai.AiStudyPlanPromptBuilder;
import com.studyplanner.backend.ai.OpenRouterClient;
import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.StudyPlan;
import com.studyplanner.backend.entity.StudySession;
import com.studyplanner.backend.entity.Subject;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.repository.StudyPlanRepository;
import com.studyplanner.backend.repository.StudySessionRepository;
import com.studyplanner.backend.repository.SubjectRepository;
import com.studyplanner.backend.repository.UserRepository;
import com.studyplanner.backend.service.AiStudyPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiStudyPlanServiceImpl implements AiStudyPlanService {

    // ── Repositories ────────────────────────────────────────────────
    private final StudyPlanRepository    studyPlanRepository;
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository      subjectRepository;
    private final UserRepository         userRepository;

    // ── AI Layer ────────────────────────────────────────────────────
    private final OpenRouterClient         openRouterClient;
    private final AiStudyPlanPromptBuilder promptBuilder;
    private final AiStudyPlanParser        aiStudyPlanParser;

    // Review session inserted every N days (spaced repetition)
    private static final int REVIEW_INTERVAL = 5;

    // ================================================================
    //  PUBLIC API
    // ================================================================

    /**
     * Calls OpenRouter AI with the user's StudyPlanRequest, parses the AI response,
     * persists the plan and auto-generated sessions, then returns the full response.
     *
     * Flow: request → build prompt → call AI → parse JSON → save plan + sessions → return
     */
    @Override
    public StudyPlanResponse generatePlan(UUID userId, StudyPlanRequest request) {
        User user = fetchUser(userId);
        StudyPlanRequest aiPlan = callAi(request);          // real AI call
        StudyPlan plan = buildPlan(user, null, aiPlan);
        return saveAndReturn(plan, aiPlan.getTopics());
    }

    /**
     * Same as generatePlan but links the resulting plan to an existing Subject entity.
     */
    @Override
    public StudyPlanResponse generatePlanWithSubject(UUID userId, UUID subjectId,
                                                     StudyPlanRequest request) {
        User    user    = fetchUser(userId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject not found with id: " + subjectId));

        StudyPlanRequest aiPlan = callAi(request);          // real AI call
        StudyPlan plan = buildPlan(user, subject, aiPlan);
        return saveAndReturn(plan, aiPlan.getTopics());
    }

    // ================================================================
    //  AI INTERACTION
    // ================================================================

    /**
     * Builds prompt from the incoming StudyPlanRequest,
     * calls OpenRouter, and parses the JSON reply back into a StudyPlanRequest.
     *
     * The user's startDate and endDate are always enforced —
     * AI can only decide title, topics, and difficulty (not dates).
     */
    private StudyPlanRequest callAi(StudyPlanRequest request) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt   = promptBuilder.buildUserPrompt(request);

        log.info("Calling OpenRouter AI — title: '{}', goal: '{}', {} → {}",
                request.getTitle(), request.getGoal(),
                request.getStartDate(), request.getEndDate());

        String aiResponse = openRouterClient.chat(systemPrompt, userPrompt);
        StudyPlanRequest aiPlan = aiStudyPlanParser.parse(aiResponse);

        // Always use the dates the USER provided — AI must not override them
        aiPlan.setStartDate(request.getStartDate());
        aiPlan.setEndDate(request.getEndDate());

        return aiPlan;
    }

    // ================================================================
    //  ENTITY BUILDING & PERSISTENCE
    // ================================================================

    /**
     * Converts the AI-parsed StudyPlanRequest into a StudyPlan entity.
     * All dates, title, difficulty, and dailyHours come from the AI response.
     */
    private StudyPlan buildPlan(User user, Subject subject, StudyPlanRequest aiPlan) {
        StudyPlan.Difficulty difficulty;
        try {
            difficulty = StudyPlan.Difficulty.valueOf(aiPlan.getDifficulty().toUpperCase());
        } catch (IllegalArgumentException e) {
            difficulty = StudyPlan.Difficulty.MEDIUM;   // safe fallback
        }

        return StudyPlan.builder()
                .user(user)
                .subject(subject)
                .title(aiPlan.getTitle())
                .goal(aiPlan.getGoal())
                .difficulty(difficulty)
                .startDate(aiPlan.getStartDate())
                .endDate(aiPlan.getEndDate())
                .dailyHours(aiPlan.getDailyHours())
                .status(StudyPlan.Status.ACTIVE)
                .build();
    }

    /**
     * Saves the plan, auto-generates sessions from AI-returned topics,
     * updates session counts, and returns the full response.
     */
    private StudyPlanResponse saveAndReturn(StudyPlan plan, List<String> topics) {
        StudyPlan savedPlan = studyPlanRepository.save(plan);

        List<StudySession> sessions      = generateSessions(savedPlan, topics);
        List<StudySession> savedSessions = studySessionRepository.saveAll(sessions);

        savedPlan.setTotalSessions(savedSessions.size());
        savedPlan.setCompletedSessions(0);
        studyPlanRepository.save(savedPlan);

        log.info("AI study plan '{}' created: {} sessions ({} → {})",
                savedPlan.getTitle(), savedSessions.size(),
                savedPlan.getStartDate(), savedPlan.getEndDate());

        return mapToResponse(savedPlan, savedSessions);
    }

    // ================================================================
    //  SESSION GENERATION  (spaced-repetition algorithm)
    // ================================================================

    /**
     * Distributes AI-generated topics across the plan's date range.
     *
     * Algorithm:
     *   1. Session duration: EASY=60 min, MEDIUM=45 min, HARD=30 min
     *   2. Sessions per day = (dailyHours × 60) / sessionDuration
     *   3. Topics round-robined across all slots (spaced repetition)
     *   4. Every REVIEW_INTERVAL days the first slot becomes a review session
     */
    private List<StudySession> generateSessions(StudyPlan plan, List<String> topics) {
        List<StudySession> sessions = new ArrayList<>();

        long totalDays = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        if (totalDays <= 0) totalDays = 1;

        int sessionMinutes = switch (plan.getDifficulty()) {
            case EASY   -> 60;
            case MEDIUM -> 45;
            case HARD   -> 30;
        };

        int sessionsPerDay = Math.max(1, (plan.getDailyHours() * 60) / sessionMinutes);

        boolean hasTopics = topics != null && !topics.isEmpty();
        Queue<String> topicQueue   = buildTopicQueue(topics, (int) totalDays, sessionsPerDay);
        List<String> coveredTopics = new ArrayList<>();

        for (int day = 0; day < totalDays; day++) {
            LocalDate scheduledDate = plan.getStartDate().plusDays(day);
            boolean isReviewDay = hasTopics
                    && (day + 1) % REVIEW_INTERVAL == 0
                    && !coveredTopics.isEmpty();

            for (int slot = 0; slot < sessionsPerDay; slot++) {
                String topic;

                if (isReviewDay && slot == 0) {
                    topic = "📝 Review: " + buildReviewSummary(coveredTopics);
                } else if (!topicQueue.isEmpty()) {
                    topic = topicQueue.poll();
                    if (hasTopics && !coveredTopics.contains(topic)) {
                        coveredTopics.add(topic);
                    }
                } else {
                    topic = "Day " + (day + 1) + " – Session " + (slot + 1);
                }

                sessions.add(StudySession.builder()
                        .plan(plan)
                        .scheduledDate(scheduledDate)
                        .topic(topic)
                        .durationMinutes(sessionMinutes)
                        .completed(false)
                        .build());
            }
        }

        return sessions;
    }

    /** Round-robin topic queue, reserving one slot per review day. */
    private Queue<String> buildTopicQueue(List<String> topics, int totalDays, int sessionsPerDay) {
        Queue<String> queue = new LinkedList<>();
        if (topics == null || topics.isEmpty()) return queue;

        int totalSlots     = totalDays * sessionsPerDay;
        int reviewSlots    = totalDays / REVIEW_INTERVAL;
        int availableSlots = totalSlots - reviewSlots;

        for (int i = 0; i < availableSlots; i++) {
            queue.add(topics.get(i % topics.size()));
        }
        return queue;
    }

    /** Shows up to 4 covered topics, then "and X more". */
    private String buildReviewSummary(List<String> coveredTopics) {
        if (coveredTopics.size() <= 4) return String.join(", ", coveredTopics);
        List<String> shown = coveredTopics.subList(0, 4);
        int remaining = coveredTopics.size() - 4;
        return String.join(", ", shown) + " and " + remaining + " more";
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private User fetchUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    // ================================================================
    //  MAPPERS
    // ================================================================

    private StudyPlanResponse mapToResponse(StudyPlan plan, List<StudySession> sessions) {
        int total     = plan.getTotalSessions()     != null ? plan.getTotalSessions()     : 0;
        int completed = plan.getCompletedSessions() != null ? plan.getCompletedSessions() : 0;
        int progress  = total > 0 ? (completed * 100) / total : 0;

        return StudyPlanResponse.builder()
                .id(plan.getId())
                .userId(plan.getUser().getId())
                .subjectId(plan.getSubject()   != null ? plan.getSubject().getId()   : null)
                .subjectName(plan.getSubject() != null ? plan.getSubject().getName() : null)
                .title(plan.getTitle())
                .goal(plan.getGoal())
                .difficulty(plan.getDifficulty().name())
                .status(plan.getStatus().name())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .dailyHours(plan.getDailyHours())
                .totalSessions(total)
                .completedSessions(completed)
                .progressPercent(progress)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .sessions(sessions.stream()
                        .map(this::mapSessionToResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private StudySessionResponse mapSessionToResponse(StudySession session) {
        return StudySessionResponse.builder()
                .id(session.getId())
                .planId(session.getPlan().getId())
                .scheduledDate(session.getScheduledDate())
                .topic(session.getTopic())
                .durationMinutes(session.getDurationMinutes())
                .completed(session.getCompleted())
                .actualDurationMinutes(session.getActualDurationMinutes())
                .focusScore(session.getFocusScore())
                .notes(session.getNotes())
                .completedAt(session.getCompletedAt())
                .build();
    }
}
