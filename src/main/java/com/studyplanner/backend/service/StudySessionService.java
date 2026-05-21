package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.SessionCompleteRequest;
import com.studyplanner.backend.dto.request.StudySessionRequest;
import com.studyplanner.backend.dto.response.StudySessionResponse;

import java.util.List;
import java.util.UUID;

public interface StudySessionService {

    StudySessionResponse createSession(
            StudySessionRequest request
    );

    StudySessionResponse getSessionById(
            UUID sessionId
    );

    List<StudySessionResponse> getSessionsByPlan(
            UUID planId
    );

    StudySessionResponse completeSession(
            UUID sessionId,
            SessionCompleteRequest request
    );

    void deleteSession(UUID sessionId);
}