package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.ai.AiStudyPlanParser;
import com.studyplanner.backend.ai.AiStudyPlanPromptBuilder;
import com.studyplanner.backend.ai.OpenRouterClient;
import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.*;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.mapper.StudyPlanMapper;
import com.studyplanner.backend.mapper.StudySessionMapper;
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
public class  AiStudyPlanServiceImpl implements AiStudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    private final OpenRouterClient openRouterClient;
    private final AiStudyPlanPromptBuilder promptBuilder;
    private final AiStudyPlanParser aiStudyPlanParser;

    private final StudyPlanMapper studyPlanMapper;
    private final StudySessionMapper studySessionMapper;

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
    public StudyPlanResponse generatePlanWithSubject(UUID subjectId, StudyPlanRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        User user = subject.getUser();
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
    private StudyPlanResponse initAndSave(User user,
                                           Subject subject,
                                           StudyPlanRequest aiPlan) {

        Optional<StudyPlan> existingPlanOpt = studyPlanRepository.findByUser_IdAndTitle(user.getId(), aiPlan.getTitle());

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

        // Fetch all sessions currently associated with the plan (completed preserved + new sessions)
        List<StudySession> allSessions = studySessionRepository.findByPlanId(plan.getId());

        StudyPlanResponse response = studyPlanMapper.toResponse(plan);
        response.setSessions(
                allSessions.stream()
                        .map(studySessionMapper::toResponse)
                        .collect(Collectors.toList())
        );

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



    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}