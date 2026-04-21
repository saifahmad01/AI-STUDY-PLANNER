package com.studyplanner.backend.controller;

import com.studyplanner.backend.dto.request.SessionCompleteRequest;
import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    // ── CREATE ────────────────────────────────────────────────────

    /**
     * POST /api/v1/plans?userId=...
     * Creates a new study plan and auto-generates sessions.
     */
    @PostMapping
    public ResponseEntity<StudyPlanResponse> createPlan(
            @RequestParam UUID userId,
            @Valid @RequestBody StudyPlanRequest request) {
        StudyPlanResponse response = studyPlanService.createPlan(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── READ ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/plans/{planId}
     * Get a single plan by ID (includes its sessions).
     */
    @GetMapping("/{planId}")
    public ResponseEntity<StudyPlanResponse> getPlanById(@PathVariable UUID planId) {
        return ResponseEntity.ok(studyPlanService.getPlanById(planId));
    }

    /**
     * GET /api/v1/plans/user/{userId}
     * Get all plans for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StudyPlanResponse>> getAllPlansByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(studyPlanService.getAllPlansByUser(userId));
    }

    /**
     * GET /api/v1/plans/user/{userId}/status?status=ACTIVE
     * Get plans filtered by status.
     */
    @GetMapping("/user/{userId}/status")
    public ResponseEntity<List<StudyPlanResponse>> getPlansByStatus(
            @PathVariable UUID userId,
            @RequestParam String status) {
        return ResponseEntity.ok(studyPlanService.getPlansByUserAndStatus(userId, status));
    }

    /**
     * GET /api/v1/plans/{planId}/sessions
     * Get all sessions for a specific plan.
     */
    @GetMapping("/{planId}/sessions")
    public ResponseEntity<List<StudySessionResponse>> getSessionsByPlan(@PathVariable UUID planId) {
        return ResponseEntity.ok(studyPlanService.getSessionsByPlan(planId));
    }

    // ── UPDATE ───────────────────────────────────────────────────

    /**
     * PUT /api/v1/plans/{planId}
     * Update a plan (regenerates sessions if dates/difficulty change).
     */
    @PutMapping("/{planId}")
    public ResponseEntity<StudyPlanResponse> updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody StudyPlanRequest request) {
        return ResponseEntity.ok(studyPlanService.updatePlan(planId, request));
    }

    // ── DELETE ───────────────────────────────────────────────────

    /**
     * DELETE /api/v1/plans/{planId}
     * Delete a plan and all its sessions.
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID planId) {
        studyPlanService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }

    // ── SESSION COMPLETION ───────────────────────────────────────

    /**
     * PATCH /api/v1/plans/sessions/{sessionId}/complete
     * Mark a session as completed with optional feedback.
     */
    @PatchMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<StudySessionResponse> completeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionCompleteRequest request) {
        return ResponseEntity.ok(
                studyPlanService.completeSession(sessionId,
                        request.getActualDurationMinutes(),
                        request.getFocusScore(),
                        request.getNotes()));
    }
}
