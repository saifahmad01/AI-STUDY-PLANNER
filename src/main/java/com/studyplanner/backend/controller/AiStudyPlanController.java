package com.studyplanner.backend.controller;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.service.AiStudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for AI-powered study plan generation.
 *
 * Base URL: /api/v1/plans/ai
 *
 * Endpoints:
 *   POST /generate                      → generate plan (no subject link)
 *   POST /generate/subject/{subjectId}  → generate plan linked to a subject
 *
 * Both endpoints accept a StudyPlanRequest body.
 * The title and topics fields are used to build the AI prompt.
 * The AI then determines the final plan details (dates, topics, sessions).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plans/ai")
@RequiredArgsConstructor
public class AiStudyPlanController {

    private final AiStudyPlanService aiStudyPlanService;

    // ================================================================
    //  POST /api/v1/plans/ai/generate?userId=...
    // ================================================================

    /**
     * Let the AI generate a complete study plan from a StudyPlanRequest.
     *
     * Key fields used for AI:
     *   - title       → subject to study (e.g. "Machine Learning")
     *   - goal        → learning goal
     *   - dailyHours  → hours available per day
     *   - difficulty  → EASY | MEDIUM | HARD
     *   - topics      → optional focus areas (e.g. ["neural networks", "CNNs"])
     *
     * @param userId  UUID of the authenticated user (query param)
     * @param request Study plan parameters
     * @return 201 Created with full StudyPlanResponse (including AI-generated sessions)
     */
    @PostMapping("/generate")
    public ResponseEntity<StudyPlanResponse> generatePlan(
            @RequestParam UUID userId,
            @Valid @RequestBody StudyPlanRequest request) {

        log.info("AI plan requested — userId={}, title='{}', difficulty={}",
                userId, request.getTitle(), request.getDifficulty());

        StudyPlanResponse response = aiStudyPlanService.generatePlan(userId, request);

        log.info("AI plan created — planId={}, sessions={}",
                response.getId(), response.getTotalSessions());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    //  POST /api/v1/plans/ai/generate/subject/{subjectId}?userId=...
    // ================================================================

    /**
     * Same as /generate but links the resulting plan to an existing Subject entity.
     *
     * @param userId    UUID of the authenticated user (query param)
     * @param subjectId UUID of the existing Subject to link (path variable)
     * @param request   Study plan parameters
     * @return 201 Created with full StudyPlanResponse (including AI-generated sessions)
     */
    @PostMapping("/generate/subject/{subjectId}")
    public ResponseEntity<StudyPlanResponse> generatePlanWithSubject(
            @RequestParam UUID userId,
            @PathVariable UUID subjectId,
            @Valid @RequestBody StudyPlanRequest request) {

        log.info("AI plan with subject requested — userId={}, subjectId={}, title='{}'",
                userId, subjectId, request.getTitle());

        StudyPlanResponse response =
                aiStudyPlanService.generatePlanWithSubject(userId, subjectId, request);

        log.info("AI plan with subject created — planId={}, sessions={}",
                response.getId(), response.getTotalSessions());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}