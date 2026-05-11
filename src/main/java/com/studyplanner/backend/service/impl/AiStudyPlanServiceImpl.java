package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.ai.AiStudyPlanParser;
import com.studyplanner.backend.ai.AiStudyPlanPromptBuilder;
import com.studyplanner.backend.ai.OpenRouterClient;
import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.*;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.repository.*;
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
public class AiStudyPlanServiceImpl implements AiStudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    private final OpenRouterClient openRouterClient;
    private final AiStudyPlanPromptBuilder promptBuilder;
    private final AiStudyPlanParser aiStudyPlanParser;

    private static final int REVIEW_DAYS = 5;

    @Override
    @Transactional
    public StudyPlanResponse generatePlan(UUID userId, StudyPlanRequest request) {
        User user = getUser(userId);
        StudyPlanRequest aiDetails = getAiPlan(request);
        return initAndSave(user, null, aiDetails);
    }

    @Override
    @Transactional
    public StudyPlanResponse generatePlanWithSubject(UUID userId, UUID subjectId, StudyPlanRequest request) {
        User user = getUser(userId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        StudyPlanRequest aiDetails = getAiPlan(request);
        return initAndSave(user, subject, aiDetails);
    }

    // Orchestrates calling AI and parsing response
    private StudyPlanRequest getAiPlan(StudyPlanRequest request) {
        log.info("Requesting AI breakdown for plan: {}", request.getTitle());

        String aiJson = openRouterClient.chat(
                promptBuilder.buildSystemPrompt(),
                promptBuilder.buildUserPrompt(request)
        );

        StudyPlanRequest result = aiStudyPlanParser.parse(aiJson);

        // Keep original dates; don't let AI change the timeline
        result.setStartDate(request.getStartDate());
        result.setEndDate(request.getEndDate());
        return result;
    }

    // Saves plan and creates the associated sessions
    private StudyPlanResponse initAndSave(User user, Subject subject, StudyPlanRequest aiPlan) {
        StudyPlan plan = mapToEntity(user, subject, aiPlan);
        StudyPlan saved = studyPlanRepository.save(plan);

        List<StudySession> sessions = createSchedule(saved, aiPlan.getTopics());
        studySessionRepository.saveAll(sessions);

        // Update counts after sessions are generated
        saved.setTotalSessions(sessions.size());
        saved.setCompletedSessions(0);

        return mapToResponse(studyPlanRepository.save(saved), sessions);
    }

    // Distributes topics across the available dates
    private List<StudySession> createSchedule(StudyPlan plan, List<String> topics) {
        List<StudySession> schedule = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;

        // Session length based on difficulty "stamina"
        int sessionMins = switch (plan.getDifficulty()) {
            case EASY -> 60;
            case HARD -> 30;
            default -> 45;
        };

        int perDay = Math.max(1, (plan.getDailyHours() * 60) / sessionMins);
        List<String> seen = new ArrayList<>();
        int topicIdx = 0;

        for (int i = 0; i < (int) days; i++) {
            LocalDate date = plan.getStartDate().plusDays(i);
            boolean isReview = (i + 1) % REVIEW_DAYS == 0 && !seen.isEmpty();

            for (int slot = 0; slot < perDay; slot++) {
                String title;

                if (isReview && slot == 0) {
                    title = "Review: " + getReviewSummary(seen);
                } else if (topics != null && !topics.isEmpty()) {
                    title = topics.get(topicIdx % topics.size());
                    if (!seen.contains(title)) seen.add(title);
                    topicIdx++;
                } else {
                    title = "Study Session " + (slot + 1);
                }

                schedule.add(StudySession.builder()
                        .plan(plan).scheduledDate(date).topic(title)
                        .durationMinutes(sessionMins).completed(false).build());
            }
        }
        return schedule;
    }

    private String getReviewSummary(List<String> seen) {
        if (seen.size() <= 3) return String.join(", ", seen);
        return seen.get(0) + ", " + seen.get(1) + " and " + (seen.size() - 2) + " more";
    }

    private StudyPlan mapToEntity(User user, Subject subject, StudyPlanRequest dto) {
        StudyPlan.Difficulty diff;
        try {
            diff = StudyPlan.Difficulty.valueOf(dto.getDifficulty().toUpperCase());
        } catch (Exception e) {
            diff = StudyPlan.Difficulty.MEDIUM; // Fallback for AI hallucinations
        }

        return StudyPlan.builder()
                .user(user).subject(subject).title(dto.getTitle()).goal(dto.getGoal())
                .difficulty(diff).startDate(dto.getStartDate()).endDate(dto.getEndDate())
                .dailyHours(dto.getDailyHours()).status(StudyPlan.Status.ACTIVE).build();
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private StudyPlanResponse mapToResponse(StudyPlan p, List<StudySession> s) {
        int total = p.getTotalSessions() != null ? p.getTotalSessions() : 0;
        int comp = p.getCompletedSessions() != null ? p.getCompletedSessions() : 0;

        return StudyPlanResponse.builder()
                .id(p.getId()).userId(p.getUser().getId())
                .subjectId(p.getSubject() != null ? p.getSubject().getId() : null)
                .subjectName(p.getSubject() != null ? p.getSubject().getName() : null)
                .title(p.getTitle()).goal(p.getGoal())
                .difficulty(p.getDifficulty().name()).status(p.getStatus().name())
                .startDate(p.getStartDate()).endDate(p.getEndDate())
                .dailyHours(p.getDailyHours()).totalSessions(total)
                .completedSessions(comp).progressPercent(total > 0 ? (comp * 100) / total : 0)
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .sessions(s.stream().map(this::toSessionDto).collect(Collectors.toList()))
                .build();
    }

    private StudySessionResponse toSessionDto(StudySession s) {
        return StudySessionResponse.builder()
                .id(s.getId()).planId(s.getPlan().getId()).scheduledDate(s.getScheduledDate())
                .topic(s.getTopic()).durationMinutes(s.getDurationMinutes()).completed(s.getCompleted())
                .actualDurationMinutes(s.getActualDurationMinutes()).focusScore(s.getFocusScore())
                .notes(s.getNotes()).completedAt(s.getCompletedAt()).build();
    }
}