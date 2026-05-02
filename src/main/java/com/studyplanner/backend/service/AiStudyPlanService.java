package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;

import java.util.UUID;

public interface AiStudyPlanService {
    StudyPlanResponse generatePlan(UUID userId, StudyPlanRequest request);
    StudyPlanResponse generatePlanWithSubject(UUID userId, UUID subjectId, StudyPlanRequest request);
}