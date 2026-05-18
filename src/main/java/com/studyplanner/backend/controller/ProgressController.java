package com.studyplanner.backend.controller;

import com.studyplanner.backend.dto.request.ProgressRequest;
import com.studyplanner.backend.dto.response.ProgressResponse;
import com.studyplanner.backend.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/{userId}")
    public ResponseEntity<ProgressResponse> trackProgress(
            @PathVariable UUID userId,
            @Valid @RequestBody ProgressRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(progressService.trackProgress(userId, request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<ProgressResponse>> getUserProgress(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                progressService.getUserProgress(userId)
        );
    }

    @GetMapping("/{userId}/daily")
    public ResponseEntity<List<ProgressResponse>> getDailyProgress(
            @PathVariable UUID userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(
                progressService.getDailyProgress(userId, date)
        );
    }

    @GetMapping("/{userId}/completed-count")
    public ResponseEntity<Long> getCompletedSessions(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                progressService.getCompletedSessions(userId)
        );
    }
}