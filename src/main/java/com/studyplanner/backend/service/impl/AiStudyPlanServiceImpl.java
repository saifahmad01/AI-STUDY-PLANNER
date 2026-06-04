package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.ai.AiStudyPlanParser;
import com.studyplanner.backend.ai.AiStudyPlanPromptBuilder;
import com.studyplanner.backend.ai .OpenRouterClient;
import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.*;
import com.studyplanner.backend.exception.BadRequestException;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.exception.DuplicateResourceException;
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

    private final OpenRouterClient openRouterClient;
    private final AiStudyPlanPromptBuilder promptBuilder;
    private final AiStudyPlanParser aiStudyPlanParser;

    private final StudyPlanMapper studyPlanMapper;
    private final StudySessionMapper studySessionMapper;

    private final ApplicationContext applicationContext;

    private static final int REVIEW_DAYS = 5;

    @Override
    public StudyPlanResponse generatePlan(UUID userId, StudyPlanRequest request) {
        User user = getUser(userId);

        // 1. Service layer validation (No subject checks)
        if (request.getGoal() == null || request.getGoal().isBlank()) {
            throw new BadRequestException("Goal is required and cannot be empty");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        // 2. Pure Global Duplicate Check (Subject is always null)
        boolean titleExists = studyPlanRepository.existsByUserIdAndSubjectIsNullAndTitleIgnoreCase(
                user.getId(),
                request.getTitle().trim()
        );

        if (titleExists) {
            throw new DuplicateResourceException("A global study plan with the same title already exists.");
        }

        // 3. AI Break down processing
        StudyPlanRequest aiDetails = getAiPlan(request);

        // 4. Self-proxy call to keep transaction context intact (Passing 'null' explicitly for subject)
        AiStudyPlanServiceImpl self = applicationContext.getBean(AiStudyPlanServiceImpl.class);
        return self.initAndSave(user, null, aiDetails);
    }

    @Override
    public StudyPlanResponse generatePlanWithSubject(UUID subjectId, StudyPlanRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        validateTitleRelevance(request.getTitle(), subject);

        User user = subject.getUser();

        // Service layer validation
        if (request.getGoal() == null || request.getGoal().isBlank()) {
            throw new BadRequestException("Goal is required and cannot be empty");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

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
        return self.initAndSave(user, subject, aiDetails);
    }

    @Override
    public StudyPlanResponse getPlanById(UUID planId) {

        StudyPlan studyPlan = studyPlanRepository.findById(planId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
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

        List<StudyPlan> studyPlans =
                studyPlanRepository.findBySubjectId(subjectId);

        if (studyPlans.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No study plans found for subject id: " + subjectId);
        }

        return studyPlans.stream()
                .map(studyPlanMapper::toResponse)
                .toList();
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
                                         StudyPlanRequest aiPlan) {

        Optional<StudyPlan> existingPlanOpt = studyPlanRepository.findByUser_IdAndTitle(user.getId(),
                aiPlan.getTitle());

        StudyPlan plan;
        List<StudySession> newSessions;
        int completedCount = 0;

        if (existingPlanOpt.isPresent()) {
            plan = existingPlanOpt.get();
            // Increment version number
            plan.setVersion(plan.getVersion() + 1);

            // Update plan details with new AI plan details using mapper
            studyPlanMapper.updateEntityFromDto(aiPlan, subject, plan);

            // Find and delete the pending (uncompleted) sessions
            List<StudySession> pendingSessions = studySessionRepository.findByPlanIdAndCompleted(plan.getId(), false);
            studySessionRepository.deleteAll(pendingSessions);

            // Count preserved completed sessions
            completedCount = (int) studySessionRepository.countByPlanIdAndCompleted(plan.getId(), true);

            // Generate a fresh schedule
            newSessions = createSchedule(plan, aiPlan.getTopics());
            studySessionRepository.saveAll(newSessions);

            // Update total & completed sessions count
            plan.setTotalSessions(completedCount + newSessions.size());
            plan.setCompletedSessions(completedCount);

            plan = studyPlanRepository.save(plan);

        } else {
            // Plan does not exist: create a new plan using mapper
            plan = studyPlanMapper.toEntity(aiPlan, user, subject);

            plan = studyPlanRepository.save(plan);

            newSessions = createSchedule(plan, aiPlan.getTopics());
            studySessionRepository.saveAll(newSessions);

            plan.setTotalSessions(newSessions.size());
            plan.setCompletedSessions(0);

            plan = studyPlanRepository.save(plan);
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
                title
        );

        log.info("Checking semantic relevance of title '{}' to subject '{}'", title, subject.getName());
        String aiResponse = openRouterClient.chat(systemPrompt, userPrompt);

        if (aiResponse == null || !aiResponse.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "").equals("true")) {
            throw new BadRequestException(String.format(
                    "The study plan title '%s' is not relevant to the subject '%s' (Category: %s).",
                    title,
                    subject.getName(),
                    subject.getCategory() != null ? subject.getCategory() : "None"
            ));
        }
    }
}