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

@Slf4j
@RestController
@RequestMapping("/api/v1/plans/ai")
@RequiredArgsConstructor
public class AiStudyPlanController {

    private final AiStudyPlanService aiStudyPlanService;


    //  POST /api/v1/plans/ai/generate?userId=...


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


    //  POST /api/v1/plans/ai/generate/subject/{subjectId}?userId=...



    @PostMapping("/generate/subject/{subjectId}")
    public ResponseEntity<StudyPlanResponse> generatePlanWithSubject(
            @PathVariable UUID subjectId,
            @Valid @RequestBody StudyPlanRequest request) {

        log.info("AI plan with subject requested — subjectId={}, title='{}'",
                subjectId, request.getTitle());

        StudyPlanResponse response =
                aiStudyPlanService.generatePlanWithSubject(subjectId, request);

        log.info("AI plan with subject created — planId={}, sessions={}",
                response.getId(), response.getTotalSessions());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}