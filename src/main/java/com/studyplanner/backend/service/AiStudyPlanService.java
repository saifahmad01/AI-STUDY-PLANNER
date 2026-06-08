package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanHistoryResponse;
import com.studyplanner.backend.dto.response.StudyPlanResponse;

import java.util.List;
import java.util.UUID;

public interface AiStudyPlanService {
    StudyPlanResponse generatePlan(UUID userId, StudyPlanRequest request);

    StudyPlanResponse generatePlanWithSubject(UUID subjectId, StudyPlanRequest request);

    StudyPlanResponse getPlanById(UUID planId);

    List<StudyPlanResponse> getPlansByUserId(UUID userId);

    List<StudyPlanResponse> getPlansBySubjectId(UUID subjectId);

    StudyPlanResponse updatePlan(UUID planId, StudyPlanRequest request);

    void deletePlan(UUID planId);

    // ── Versioning & Rollback ───────────────────────────────────────────

    /** Get the full version history for a plan, newest first. */
    List<StudyPlanHistoryResponse> getPlanHistory(UUID planId);

    /** Get a single specific version snapshot. */
    StudyPlanHistoryResponse getPlanVersion(UUID planId, int versionNumber);

    /** Rollback the plan to a specific version, returning the restored state. */
    StudyPlanResponse rollbackPlan(UUID planId, int targetVersion);
}