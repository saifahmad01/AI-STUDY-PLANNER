package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.ai.AiStudyPlanParser;
import com.studyplanner.backend.ai.AiStudyPlanPromptBuilder;
import com.studyplanner.backend.ai.OpenRouterClient;
import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanHistoryResponse;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.*;
import com.studyplanner.backend.exception.BadRequestException;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.exception.DuplicateResourceException;
import com.studyplanner.backend.mapper.StudyPlanHistoryMapper;
import com.studyplanner.backend.mapper.StudyPlanMapper;
import com.studyplanner.backend.mapper.StudySessionMapper;
import com.studyplanner.backend.repository.*;
import com.studyplanner.backend.service.AiStudyPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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

    private final StudyPlanHistoryRepository studyPlanHistoryRepository;
    private final StudySessionHistoryRepository studySessionHistoryRepository;

    private final OpenRouterClient openRouterClient;
    private final AiStudyPlanPromptBuilder promptBuilder;
    private final AiStudyPlanParser aiStudyPlanParser;

    private final StudyPlanMapper studyPlanMapper;
    private final StudySessionMapper studySessionMapper;
    private final StudyPlanHistoryMapper studyPlanHistoryMapper;

    private final ApplicationContext applicationContext;

    private static final int REVIEW_DAYS = 5;

    // ════════════════════════════════════════════════════════════════════
    //  Existing CRUD methods (unchanged)
    // ════════════════════════════════════════════════════════════════════

    @Override
    public StudyPlanResponse generatePlan(UUID userId, StudyPlanRequest request) {
        User user = getUser(userId);

        // 1. Service layer validation (No subject checks)

        // 2. Pure Global Duplicate Check (Subject is always null)
        boolean titleExists = studyPlanRepository.existsByUserIdAndSubjectIsNullAndTitleIgnoreCase(
                user.getId(),
                request.getTitle().trim());

        if (titleExists) {
            throw new DuplicateResourceException("A global study plan with the same title already exists.");
        }

        // 3. AI Break down processing
        StudyPlanRequest aiDetails = getAiPlan(request);

        // 4. Self-proxy call to keep transaction context intact (Passing 'null'
        // explicitly for subject)
        AiStudyPlanServiceImpl self = applicationContext.getBean(AiStudyPlanServiceImpl.class);
        return self.initAndSave(user, null, aiDetails, null);
    }

    @Override
    public StudyPlanResponse generatePlanWithSubject(UUID subjectId, StudyPlanRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        validateTitleRelevance(request.getTitle(), subject);

        User user = subject.getUser();

        // Service layer validation
        validateRequestDates(request);

        if (studyPlanRepository.existsByUser_IdAndSubject_IdAndTitleIgnoreCase(
                user.getId(),
                subject.getId(),
                request.getTitle().trim())) {

            throw new DuplicateResourceException(
                    "A study plan with the same title already exists for this subject.");
        }

        StudyPlanRequest aiDetails = getAiPlan(request);

        // Invoke initAndSave via self-proxy to ensure transactional context
        AiStudyPlanServiceImpl self = applicationContext.getBean(AiStudyPlanServiceImpl.class);
        return self.initAndSave(user, subject, aiDetails, null);
    }

    @Override
    public StudyPlanResponse getPlanById(UUID planId) {

        StudyPlan studyPlan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Study Plan not found with id: " + planId));

        return studyPlanMapper.toResponse(studyPlan);
    }

    @Override
    public List<StudyPlanResponse> getPlansByUserId(UUID userId) {

        return studyPlanRepository.findByUserId(userId)
                .stream()
                .map(studyPlanMapper::toResponse)
                .toList();
    }

    @Override
    public List<StudyPlanResponse> getPlansBySubjectId(UUID subjectId) {

        List<StudyPlan> studyPlans = studyPlanRepository.findBySubjectId(subjectId);

        if (studyPlans.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No study plans found for subject id: " + subjectId);
        }

        return studyPlans.stream()
                .map(studyPlanMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StudyPlanResponse updatePlan(UUID planId, StudyPlanRequest request) {
        StudyPlan existing = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Study Plan not found with id: " + planId));

        validateRequestDates(request);

        // ── Snapshot current state BEFORE applying mutations ─────────────
        snapshotCurrentState(existing);

        // Conditional title check (only if title changed)
        if (!existing.getTitle().equalsIgnoreCase(request.getTitle().trim())) {
            boolean duplicate = existing.getSubject() != null
                    ? studyPlanRepository.existsByUser_IdAndSubject_IdAndTitleIgnoreCase(existing.getUser().getId(),
                            existing.getSubject().getId(), request.getTitle().trim())
                    : studyPlanRepository.existsByUserIdAndSubjectIsNullAndTitleIgnoreCase(existing.getUser().getId(),
                            request.getTitle().trim());

            if (duplicate) {
                throw new DuplicateResourceException("A study plan with the same title already exists.");
            }

            if (existing.getSubject() != null) {
                validateTitleRelevance(request.getTitle(), existing.getSubject());
            }
        }

        StudyPlanRequest aiDetails = getAiPlan(request);
        AiStudyPlanServiceImpl self = applicationContext.getBean(AiStudyPlanServiceImpl.class);
        return self.initAndSave(existing.getUser(), existing.getSubject(), aiDetails, existing);
    }

    @Override
    @Transactional
    public void deletePlan(UUID planId) {
        if (!studyPlanRepository.existsById(planId)) {
            throw new ResourceNotFoundException("Study Plan not found with id: " + planId);
        }
        studyPlanRepository.deleteById(planId);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Versioning & Rollback (NEW)
    // ════════════════════════════════════════════════════════════════════

    @Override
    public List<StudyPlanHistoryResponse> getPlanHistory(UUID planId) {
        if (!studyPlanRepository.existsById(planId)) {
            throw new ResourceNotFoundException("Study Plan not found with id: " + planId);
        }

        List<StudyPlanHistory> history =
                studyPlanHistoryRepository.findByPlanIdOrderByVersionNumberDesc(planId);

        return studyPlanHistoryMapper.toHistoryResponseList(history);
    }

    @Override
    public StudyPlanHistoryResponse getPlanVersion(UUID planId, int versionNumber) {
        if (!studyPlanRepository.existsById(planId)) {
            throw new ResourceNotFoundException("Study Plan not found with id: " + planId);
        }

        StudyPlanHistory history = studyPlanHistoryRepository
                .findByPlanIdAndVersionNumber(planId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for plan: " + planId));

        return studyPlanHistoryMapper.toHistoryResponse(history);
    }

    @Override
    @Transactional
    public StudyPlanResponse rollbackPlan(UUID planId, int targetVersion) {
        StudyPlan existing = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Study Plan not found with id: " + planId));

        // 1. Snapshot the current state first (so this rollback is itself reversible)
        snapshotCurrentState(existing);
        log.info("Snapshotted current state before rollback — planId={}", planId);

        // 2. Load the target version
        StudyPlanHistory target = studyPlanHistoryRepository
                .findByPlanIdAndVersionNumber(planId, targetVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + targetVersion + " not found for plan: " + planId));

        // 3. Copy fields from the history snapshot back onto the live plan
        existing.setTitle(target.getTitle());
        existing.setGoal(target.getGoal());
        existing.setDifficulty(StudyPlan.Difficulty.valueOf(target.getDifficulty()));
        existing.setStartDate(target.getStartDate());
        existing.setEndDate(target.getEndDate());
        existing.setDailyHours(target.getDailyHours());
        existing.setStatus(StudyPlan.Status.valueOf(target.getStatus()));
        existing.setChangeReason("Rolled back to version " + targetVersion);

        // 4. Delete ALL current sessions and re-create from the snapshot
        List<StudySession> currentSessions = studySessionRepository.findByPlanId(planId);
        studySessionRepository.deleteAll(currentSessions);

        List<StudySessionHistory> snapshotSessions =
                studySessionHistoryRepository.findByPlanHistoryId(target.getId());

        final StudyPlan planRef = existing;
        List<StudySession> restoredSessions = snapshotSessions.stream()
                .map(sh -> StudySession.builder()
                        .plan(planRef)
                        .scheduledDate(sh.getScheduledDate())
                        .topic(sh.getTopic())
                        .durationMinutes(sh.getDurationMinutes())
                        .completed(sh.getCompleted())
                        .actualDurationMinutes(sh.getActualDurationMinutes())
                        .focusScore(sh.getFocusScore())
                        .notes(sh.getNotes())
                        .completedAt(sh.getCompletedAt())
                        .build())
                .collect(Collectors.toList());

        studySessionRepository.saveAll(restoredSessions);

        // 5. Update session counts
        long completedCount = restoredSessions.stream()
                .filter(s -> Boolean.TRUE.equals(s.getCompleted()))
                .count();
        existing.setTotalSessions(restoredSessions.size());
        existing.setCompletedSessions((int) completedCount);

        existing = studyPlanRepository.save(existing);

        log.info("Rolled back planId={} to version {} — {} sessions restored",
                planId, targetVersion, restoredSessions.size());

        // 6. Build response
        StudyPlanResponse response = studyPlanMapper.toResponse(existing);
        response.setSessions(
                restoredSessions.stream()
                        .map(studySessionMapper::toResponse)
                        .collect(Collectors.toList()));

        return response;
    }

    // ════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ════════════════════════════════════════════════════════════════════

    /**
     * Takes a snapshot of the current study plan state and all its sessions,
     * storing them in the history tables. Called BEFORE any update or rollback
     * so the previous state is always preserved.
     */
    private void snapshotCurrentState(StudyPlan plan) {
        // Determine next version number
        int nextVersion = studyPlanHistoryRepository
                .findTopByPlanIdOrderByVersionNumberDesc(plan.getId())
                .map(h -> h.getVersionNumber() + 1)
                .orElse(1);

        // Create the plan history snapshot
        StudyPlanHistory planSnapshot = studyPlanHistoryMapper.toHistoryEntity(plan, nextVersion);
        planSnapshot = studyPlanHistoryRepository.save(planSnapshot);

        // Create session history snapshots
        List<StudySession> currentSessions = studySessionRepository.findByPlanId(plan.getId());
        StudyPlanHistory savedSnapshot = planSnapshot;

        List<StudySessionHistory> sessionSnapshots = currentSessions.stream()
                .map(session -> studyPlanHistoryMapper.toSessionHistoryEntity(session, savedSnapshot))
                .collect(Collectors.toList());

        studySessionHistoryRepository.saveAll(sessionSnapshots);

        log.info("Created history snapshot v{} for planId={} with {} sessions",
                nextVersion, plan.getId(), sessionSnapshots.size());
    }

    // Orchestrates calling AI and parsing response
    private StudyPlanRequest getAiPlan(StudyPlanRequest request) {
        log.info("Requesting AI breakdown for plan: {}", request.getTitle());

        String aiJson = openRouterClient.chat(
                promptBuilder.buildSystemPrompt(),
                promptBuilder.buildUserPrompt(request));

        StudyPlanRequest result = aiStudyPlanParser.parse(aiJson);

        // Keep original dates, title, and goal; don't let AI change these core fields
        result.setStartDate(request.getStartDate());
        result.setEndDate(request.getEndDate());
        result.setTitle(request.getTitle());
        result.setGoal(request.getGoal());
        return result;
    }

    // Saves plan and creates the associated sessions
    @Transactional
    public StudyPlanResponse initAndSave(User user,
            Subject subject,
            StudyPlanRequest aiPlan,
            StudyPlan existingPlan) {

        StudyPlan plan;
        List<StudySession> newSessions;
        int completedCount = 0;

        StudyPlan existing = existingPlan;
        if (existing == null) {
            existing = studyPlanRepository.findByUser_IdAndTitle(user.getId(), aiPlan.getTitle()).orElse(null);
        }

        if (existing != null) {
            plan = existing;

            // Find and delete the pending (uncompleted) sessions first (before dirtying the entity to avoid auto-flush traps)
            List<StudySession> pendingSessions = studySessionRepository.findByPlanIdAndCompleted(plan.getId(), false);
            studySessionRepository.deleteAll(pendingSessions);

            // Count preserved completed sessions
            completedCount = (int) studySessionRepository.countByPlanIdAndCompleted(plan.getId(), true);

            // Update plan details with new AI plan details using mapper (this dirties the entity)
            studyPlanMapper.updateEntityFromDto(aiPlan, subject, plan);

            // Generate a fresh schedule
            newSessions = createSchedule(plan, aiPlan.getTopics());

            // Set counts BEFORE the save so only ONE UPDATE is issued
            plan.setTotalSessions(completedCount + newSessions.size());
            plan.setCompletedSessions(completedCount);

            plan = studyPlanRepository.save(plan);
            studySessionRepository.saveAll(newSessions);

        } else {
            // Plan does not exist: create a new plan using mapper
            plan = studyPlanMapper.toEntity(aiPlan, user, subject);

            // Pre-compute session count from plan fields BEFORE the INSERT,
            // so we only issue a single INSERT and @Version stays at 1.
            plan.setTotalSessions(computeSessionCount(plan));
            plan.setCompletedSessions(0);

            plan = studyPlanRepository.save(plan);          // single INSERT, version stays 1

            // Now that the plan is persisted (has an ID), create sessions
            newSessions = createSchedule(plan, aiPlan.getTopics());
            studySessionRepository.saveAll(newSessions);
        }

        // Fetch all sessions currently associated with the plan (completed preserved +
        // new sessions)
        List<StudySession> allSessions = studySessionRepository.findByPlanId(plan.getId());

        StudyPlanResponse response = studyPlanMapper.toResponse(plan);
        response.setSessions(
                allSessions.stream()
                        .map(studySessionMapper::toResponse)
                        .collect(Collectors.toList()));

        return response;
    }

    /**
     * Computes the total number of sessions that createSchedule() will produce,
     * based purely on plan metadata. Used to set totalSessions BEFORE the first
     * save so that @Version is not bumped by a redundant UPDATE.
     */
    private int computeSessionCount(StudyPlan plan) {
        long days = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        int sessionMins = switch (plan.getDifficulty()) {
            case EASY -> 60;
            case HARD -> 30;
            default -> 45;
        };
        int perDay = Math.max(1, (plan.getDailyHours() * 60) / sessionMins);
        return (int) days * perDay;
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
                    if (!seen.contains(title))
                        seen.add(title);
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
        if (seen.size() <= 3)
            return String.join(", ", seen);
        return seen.get(0) + ", " + seen.get(1) + " and " + (seen.size() - 2) + " more";
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private void validateTitleRelevance(String title, Subject subject) {
        if (subject == null || title == null || title.isBlank()) {
            return;
        }

        String systemPrompt = "You are an expert educational system validator. Your task is to determine if a study plan's title is semantically relevant to its assigned subject, taking into account any associated category.\n\n" +
                "Given the following Subject Name, Subject Category, and Study Plan Title, analyze the relationship between them. Respond with ONLY 'true' if the title is relevant, or 'false' if it is not. Ensure you consider synonyms and related concepts.Analyze the relationship carefully, considering subdomains, concepts, and synonyms related to the subject. Respond with ONLY 'true' if the title is relevant, and 'false' if it is not.";

        String userPrompt = String.format(
                "Subject Name: %s\nSubject Category: %s\nStudy Plan Title: %s",
                subject.getName(),
                subject.getCategory() != null ? subject.getCategory() : "None",
                title);

        log.info("Checking semantic relevance of title '{}' to subject '{}'", title, subject.getName());
        String aiResponse = openRouterClient.chat(systemPrompt, userPrompt);

        if (aiResponse == null || !aiResponse.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "").equals("true")) {
            throw new BadRequestException(String.format(
                    "The study plan title '%s' is not relevant to the subject '%s' (Category: %s).",
                    title,
                    subject.getName(),
                    subject.getCategory() != null ? subject.getCategory() : "None"));
        }
    }

    private void validateRequestDates(StudyPlanRequest request) {
        if (request.getGoal() == null || request.getGoal().isBlank()) {
            throw new BadRequestException("Goal is required and cannot be empty");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
    }
}