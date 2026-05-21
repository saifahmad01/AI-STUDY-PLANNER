package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.dto.request.SessionCompleteRequest;
import com.studyplanner.backend.dto.request.StudySessionRequest;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.StudyPlan;
import com.studyplanner.backend.entity.StudySession;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.mapper.StudySessionMapper;
import com.studyplanner.backend.repository.StudyPlanRepository;
import com.studyplanner.backend.repository.StudySessionRepository;
import com.studyplanner.backend.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudySessionServiceImpl
        implements StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudySessionMapper studySessionMapper;

    @Override
    @Transactional
    public StudySessionResponse createSession(
            StudySessionRequest request
    ) {

        StudyPlan plan = studyPlanRepository
                .findById(request.getPlanId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plan not found: "
                                        + request.getPlanId()
                        )
                );

        StudySession session = StudySession.builder()
                .plan(plan)
                .scheduledDate(request.getScheduledDate())
                .topic(request.getTopic())
                .durationMinutes(request.getDurationMinutes())
                .completed(false)
                .build();

        StudySession saved =
                studySessionRepository.save(session);

        return studySessionMapper.toResponse(saved);
    }

    @Override
    public StudySessionResponse getSessionById(
            UUID sessionId
    ) {

        StudySession session =
                studySessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found: "
                                                + sessionId
                                )
                        );

        return studySessionMapper.toResponse(session);
    }

    @Override
    public List<StudySessionResponse> getSessionsByPlan(
            UUID planId
    ) {

        return studySessionRepository
                .findByPlanId(planId)
                .stream()
                .map(studySessionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudySessionResponse completeSession(
            UUID sessionId,
            SessionCompleteRequest request
    ) {

        StudySession session =
                studySessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found: "
                                                + sessionId
                                )
                        );

        session.setCompleted(true);
        session.setCompletedAt(LocalDateTime.now());
        session.setActualDurationMinutes(
                request.getActualDurationMinutes()
        );
        session.setFocusScore(
                request.getFocusScore()
        );
        session.setNotes(
                request.getNotes()
        );

        StudySession updated =
                studySessionRepository.save(session);

        return studySessionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId) {

        if (!studySessionRepository.existsById(sessionId)) {

            throw new ResourceNotFoundException(
                    "Session not found: " + sessionId
            );
        }

        studySessionRepository.deleteById(sessionId);
    }
}