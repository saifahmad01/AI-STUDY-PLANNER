package com.studyplanner.backend.controller;

import com.studyplanner.backend.dto.request.SubjectRequest;
import com.studyplanner.backend.dto.response.SubjectResponse;
import com.studyplanner.backend.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<SubjectResponse> createSubject(
            @PathVariable UUID userId,
            @Valid @RequestBody SubjectRequest request) {
        SubjectResponse response = subjectService.createSubject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{subjectId}")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable UUID subjectId) {
        return ResponseEntity.ok(subjectService.getSubjectById(subjectId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubjectResponse>> getAllSubjectsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(subjectService.getAllSubjectsByUser(userId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SubjectResponse>> getActiveSubjectsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(subjectService.getActiveSubjectsByUser(userId));
    }

    @PutMapping("/{subjectId}")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable UUID subjectId,
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(subjectId, request));
    }

    @PatchMapping("/{subjectId}/archive")
    public ResponseEntity<SubjectResponse> archiveSubject(@PathVariable UUID subjectId) {
        return ResponseEntity.ok(subjectService.archiveSubject(subjectId));
    }

    @DeleteMapping("/{subjectId}")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID subjectId) {
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.noContent().build();
    }
}
