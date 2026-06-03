package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;

import java.util.List;
import java.util.UUID;

public interface AiStudyPlanService {
    StudyPlanResponse generatePlan(UUID userId, StudyPlanRequest request);
    StudyPlanResponse generatePlanWithSubject(UUID subjectId, StudyPlanRequest request);
    StudyPlanResponse getPlanById(UUID planId);
    List<StudyPlanResponse> getPlansByUserId(UUID userId);
    List<StudyPlanResponse> getPlansBySubjectId(UUID subjectId);
}