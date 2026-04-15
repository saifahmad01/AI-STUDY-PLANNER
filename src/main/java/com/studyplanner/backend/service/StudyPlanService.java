package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.dto.response.StudySessionResponse;

import java.util.List;
import java.util.UUID;

public interface StudyPlanService {
    /**
     * Create a new study plan and auto-generate study sessions.
     */
    StudyPlanResponse createPlan(UUID userId, StudyPlanRequest request);

    /**
     * Get a study plan by its ID (includes generated sessions).
     */
    StudyPlanResponse getPlanById(UUID planId);

    /**
     * Get all study plans for a user.
     */
    List<StudyPlanResponse> getAllPlansByUser(UUID userId);

    /**
     * Get study plans by user and status (ACTIVE, PAUSED, etc.).
     */
    List<StudyPlanResponse> getPlansByUserAndStatus(UUID userId, String status);

    /**
     * Update an existing study plan (regenerates sessions if dates/topics change).
     */
    StudyPlanResponse updatePlan(UUID planId, StudyPlanRequest request);

    /**
     * Delete a study plan and all its sessions.
     */
    void deletePlan(UUID planId);

    /**
     * Mark a session as completed with optional feedback.
     */
    StudySessionResponse completeSession(UUID sessionId, Integer actualDurationMinutes, Short focusScore, String notes);

    /**
     * Get all sessions for a specific plan.
     */
    List<StudySessionResponse> getSessionsByPlan(UUID planId);
}