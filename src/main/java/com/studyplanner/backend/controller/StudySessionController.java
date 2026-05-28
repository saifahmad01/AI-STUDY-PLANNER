package com.studyplanner.backend.controller;

import com.studyplanner.backend.dto.request.SessionCompleteRequest;
import com.studyplanner.backend.dto.request.StudySessionRequest;
import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.service.StudySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;



    @PostMapping
    public ResponseEntity<StudySessionResponse> createSession(
            @Valid @RequestBody StudySessionRequest request
    ) {

        log.info(
                "Creating study session for planId={}, topic={}",
                request.getPlanId(),
                request.getTopic()
        );

        StudySessionResponse response =
                studySessionService.createSession(request);

        log.info(
                "Study session created successfully with id={}",
                response.getId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping("/{sessionId}")
    public ResponseEntity<StudySessionResponse> getSessionById(
            @PathVariable UUID sessionId
    ) {

        log.info(
                "Fetching study session with id={}",
                sessionId
        );

        StudySessionResponse response =
                studySessionService.getSessionById(sessionId);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<StudySessionResponse>> getSessionsByPlan(
            @PathVariable UUID planId
    ) {

        log.info(
                "Fetching all sessions for planId={}",
                planId
        );

        List<StudySessionResponse> response =
                studySessionService.getSessionsByPlan(planId);

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{sessionId}/complete")
    public ResponseEntity<StudySessionResponse> completeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionCompleteRequest request
    ) {

        log.info(
                "Completing session with id={}",
                sessionId
        );

        StudySessionResponse response =
                studySessionService.completeSession(
                        sessionId,
                        request
                );

        log.info(
                "Session completed successfully with id={}",
                sessionId
        );

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionId
    ) {

        log.info(
                "Deleting study session with id={}",
                sessionId
        );

        studySessionService.deleteSession(sessionId);

        log.info(
                "Study session deleted successfully with id={}",
                sessionId
        );

        return ResponseEntity.noContent().build();
    }
}