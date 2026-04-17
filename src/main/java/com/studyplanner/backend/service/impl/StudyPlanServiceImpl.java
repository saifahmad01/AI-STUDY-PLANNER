package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.StudyPlan;
import com.studyplanner.backend.entity.StudySession;
import com.studyplanner.backend.entity.Subject;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.BadRequestException;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.repository.StudyPlanRepository;
import com.studyplanner.backend.repository.StudySessionRepository;
import com.studyplanner.backend.repository.SubjectRepository;
import com.studyplanner.backend.repository.UserRepository;
import com.studyplanner.backend.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudyPlanServiceImpl implements StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    // ══════════════════════════════════════════════════════════════
    //  CREATE — build plan + auto-generate sessions
    // ══════════════════════════════════════════════════════════════

    @Override
    public StudyPlanResponse createPlan(UUID userId, StudyPlanRequest request) {
        // Validate dates
        validateDates(request.getStartDate(), request.getEndDate());

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Fetch subject (optional)
        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
        }

        // Build the plan entity
        StudyPlan plan = StudyPlan.builder()
                .user(user)
                .subject(subject)
                .title(request.getTitle())
                .goal(request.getGoal())
                .difficulty(StudyPlan.Difficulty.valueOf(request.getDifficulty().toUpperCase()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .dailyHours(request.getDailyHours())
                .status(StudyPlan.Status.ACTIVE)
                .build();

        StudyPlan savedPlan = studyPlanRepository.save(plan);

        // Auto-generate sessions
        List<StudySession> sessions = generateSessions(savedPlan, request.getTopics());
        List<StudySession> savedSessions = studySessionRepository.saveAll(sessions);

        // Update plan's total session count
        savedPlan.setTotalSessions(savedSessions.size());
        savedPlan.setCompletedSessions(0);
        studyPlanRepository.save(savedPlan);

        log.info("Created plan '{}' with {} auto-generated sessions", savedPlan.getTitle(), savedSessions.size());
        return mapToResponse(savedPlan, savedSessions);
    }

    // ══════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public StudyPlanResponse getPlanById(UUID planId) {
        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with id: " + planId));
        List<StudySession> sessions = studySessionRepository.findByPlanId(planId);
        return mapToResponse(plan, sessions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyPlanResponse> getAllPlansByUser(UUID userId) {
        return studyPlanRepository.findByUserId(userId).stream()
                .map(plan -> {
                    List<StudySession> sessions = studySessionRepository.findByPlanId(plan.getId());
                    return mapToResponse(plan, sessions);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyPlanResponse> getPlansByUserAndStatus(UUID userId, String status) {
        StudyPlan.Status planStatus = StudyPlan.Status.valueOf(status.toUpperCase());
        return studyPlanRepository.findByUserIdAndStatus(userId, planStatus).stream()
                .map(plan -> {
                    List<StudySession> sessions = studySessionRepository.findByPlanId(plan.getId());
                    return mapToResponse(plan, sessions);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudySessionResponse> getSessionsByPlan(UUID planId) {
        if (!studyPlanRepository.existsById(planId)) {
            throw new ResourceNotFoundException("Study plan not found with id: " + planId);
        }
        return studySessionRepository.findByPlanId(planId).stream()
                .map(this::mapSessionToResponse)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE — re-generates sessions if dates or topics change
    // ══════════════════════════════════════════════════════════════

    @Override
    public StudyPlanResponse updatePlan(UUID planId, StudyPlanRequest request) {
        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with id: " + planId));

        validateDates(request.getStartDate(), request.getEndDate());

        boolean needsRegeneration =
                !plan.getStartDate().equals(request.getStartDate()) ||
                        !plan.getEndDate().equals(request.getEndDate()) ||
                        !plan.getDailyHours().equals(request.getDailyHours()) ||
                        !plan.getDifficulty().name().equalsIgnoreCase(request.getDifficulty());

        // Update plan fields
        plan.setTitle(request.getTitle());
        plan.setGoal(request.getGoal());
        plan.setDifficulty(StudyPlan.Difficulty.valueOf(request.getDifficulty().toUpperCase()));
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setDailyHours(request.getDailyHours());

        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
            plan.setSubject(subject);
        }

        List<StudySession> sessions;

        if (needsRegeneration) {
            // Delete old sessions and regenerate
            studySessionRepository.deleteAll(studySessionRepository.findByPlanId(planId));
            sessions = generateSessions(plan, request.getTopics());
            sessions = studySessionRepository.saveAll(sessions);
            plan.setTotalSessions(sessions.size());
            plan.setCompletedSessions(0);
            log.info("Regenerated {} sessions for plan '{}'", sessions.size(), plan.getTitle());
        } else {
            sessions = studySessionRepository.findByPlanId(planId);
        }

        StudyPlan updatedPlan = studyPlanRepository.save(plan);
        return mapToResponse(updatedPlan, sessions);
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════

    @Override
    public void deletePlan(UUID planId) {
        if (!studyPlanRepository.existsById(planId)) {
            throw new ResourceNotFoundException("Study plan not found with id: " + planId);
        }
        // Delete all sessions first, then the plan
        List<StudySession> sessions = studySessionRepository.findByPlanId(planId);
        studySessionRepository.deleteAll(sessions);
        studyPlanRepository.deleteById(planId);
        log.info("Deleted plan {} and its {} sessions", planId, sessions.size());
    }

    // ══════════════════════════════════════════════════════════════
    //  COMPLETE SESSION — mark done with optional feedback
    // ══════════════════════════════════════════════════════════════

    @Override
    public StudySessionResponse completeSession(UUID sessionId, Integer actualDurationMinutes, Short focusScore, String notes) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        if (session.getCompleted()) {
            throw new BadRequestException("Session is already completed");
        }

        session.setCompleted(true);
        session.setCompletedAt(LocalDateTime.now());
        session.setActualDurationMinutes(actualDurationMinutes);
        session.setFocusScore(focusScore);
        session.setNotes(notes);

        StudySession saved = studySessionRepository.save(session);

        // Update plan's completed count
        StudyPlan plan = session.getPlan();
        long completedCount = studySessionRepository.countByPlanIdAndCompleted(plan.getId(), true);
        plan.setCompletedSessions((int) completedCount);

        // Auto-complete the plan if all sessions are done
        if (plan.getCompletedSessions().equals(plan.getTotalSessions())) {
            plan.setStatus(StudyPlan.Status.COMPLETED);
            log.info("Plan '{}' auto-completed — all sessions done!", plan.getTitle());
        }
        studyPlanRepository.save(plan);

        return mapSessionToResponse(saved);
    }

    // ══════════════════════════════════════════════════════════════
    //  SESSION GENERATION ALGORITHM
    // ══════════════════════════════════════════════════════════════

    /**
     * Generates study sessions based on plan parameters and optional topics.
     *
     * Algorithm:
     * 1. Calculate total available days between start and end date.
     * 2. Determine session duration based on difficulty:
     *    - EASY:   60 min (relaxed, longer blocks)
     *    - MEDIUM: 45 min (balanced)
     *    - HARD:   30 min (intense, shorter focused blocks)
     * 3. Calculate sessions per day = (dailyHours × 60) / sessionDuration.
     * 4. Distribute topics using round-robin with spaced repetition:
     *    - Topics are spread across days so the same topic doesn't repeat
     *      on consecutive days → better retention.
     *    - If no topics provided, generate generic "Day N - Session M" labels.
     * 5. Automatically insert a review session every 5th day.
     */
    private List<StudySession> generateSessions(StudyPlan plan, List<String> topics) {
        List<StudySession> sessions = new ArrayList<>();

        long totalDays = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;

        if (totalDays <= 0) {
            throw new BadRequestException("End date must be after start date");
        }

        // ── Step 1: Session duration based on difficulty ──
        int sessionMinutes = switch (plan.getDifficulty()) {
            case EASY   -> 60;
            case MEDIUM -> 45;
            case HARD   -> 30;
        };

        // ── Step 2: Sessions per day ──
        int sessionsPerDay = Math.max(1, (plan.getDailyHours() * 60) / sessionMinutes);

        // ── Step 3: Build topic queue with spaced repetition ──
        Queue<String> topicQueue = buildTopicQueue(topics, (int) totalDays, sessionsPerDay);
        boolean hasCustomTopics = topics != null && !topics.isEmpty();

        // ── Step 4: Review day interval (every 5th day) ──
        int reviewInterval = 5;
        List<String> coveredTopics = new ArrayList<>();

        // ── Step 5: Generate sessions day-by-day ──
        for (int day = 0; day < totalDays; day++) {
            LocalDate scheduledDate = plan.getStartDate().plusDays(day);
            boolean isReviewDay = hasCustomTopics && (day + 1) % reviewInterval == 0 && !coveredTopics.isEmpty();

            for (int slot = 0; slot < sessionsPerDay; slot++) {
                String topic;

                if (isReviewDay && slot == 0) {
                    // First slot on review day → review previously covered topics
                    topic = "📝 Review: " + buildReviewSummary(coveredTopics);
                } else if (!topicQueue.isEmpty()) {
                    topic = topicQueue.poll();
                    if (hasCustomTopics && !coveredTopics.contains(topic)) {
                        coveredTopics.add(topic);
                    }
                } else {
                    topic = "Day " + (day + 1) + " - Session " + (slot + 1);
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

    /**
     * Builds a topic queue using round-robin spaced repetition.
     * Topics are interleaved so the same topic doesn't appear on consecutive days.
     *
     * Example with topics [A, B, C] and 10 days × 2 sessions/day = 20 slots:
     *   → A, B, C, A, B, C, A, B, C, A, B, C, A, B, C, A, B, C, A, B
     */
    private Queue<String> buildTopicQueue(List<String> topics, int totalDays, int sessionsPerDay) {
        Queue<String> queue = new LinkedList<>();

        if (topics == null || topics.isEmpty()) {
            return queue; // Will fall back to generic labels
        }

        int totalSlots = totalDays * sessionsPerDay;

        // Account for review slots (every 5th day, 1 slot used for review)
        int reviewSlots = totalDays / 5;
        int availableSlots = totalSlots - reviewSlots;

        // Round-robin: cycle through topics to fill all available slots
        for (int i = 0; i < availableSlots; i++) {
            queue.add(topics.get(i % topics.size()));
        }

        return queue;
    }

    /**
     * Builds a short summary string for review sessions.
     * Shows up to 4 topics, then "and X more".
     */
    private String buildReviewSummary(List<String> coveredTopics) {
        if (coveredTopics.size() <= 4) {
            return String.join(", ", coveredTopics);
        }
        List<String> shown = coveredTopics.subList(0, 4);
        int remaining = coveredTopics.size() - 4;
        return String.join(", ", shown) + " and " + remaining + " more";
    }

    // ══════════════════════════════════════════════════════════════
    //  MAPPERS
    // ══════════════════════════════════════════════════════════════

    private StudyPlanResponse mapToResponse(StudyPlan plan, List<StudySession> sessions) {
        int total = plan.getTotalSessions() != null ? plan.getTotalSessions() : 0;
        int completed = plan.getCompletedSessions() != null ? plan.getCompletedSessions() : 0;
        int progressPercent = total > 0 ? (completed * 100) / total : 0;

        return StudyPlanResponse.builder()
                .id(plan.getId())
                .userId(plan.getUser().getId())
                .subjectId(plan.getSubject() != null ? plan.getSubject().getId() : null)
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
                .progressPercent(progressPercent)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .sessions(sessions.stream().map(this::mapSessionToResponse).collect(Collectors.toList()))
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

    // ══════════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════════

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Start date cannot be in the past");
        }
    }
}
