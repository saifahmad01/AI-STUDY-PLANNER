package com.studyplanner.backend.controller;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanHistoryResponse;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.service.AiStudyPlanService;
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
@RequestMapping("/api/v1/plans/ai")
@RequiredArgsConstructor
public class AiStudyPlanController {

	private final AiStudyPlanService aiStudyPlanService;

	// POST /api/v1/plans/ai/generate?userId=...

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

	// POST /api/v1/plans/ai/generate/subject/{subjectId}?userId=...

	@PostMapping("/generate/subject/{subjectId}")
	public ResponseEntity<StudyPlanResponse> generatePlanWithSubject(
			@PathVariable UUID subjectId,
			@Valid @RequestBody StudyPlanRequest request) {

		log.info("AI plan with subject requested — subjectId={}, title='{}'",
				subjectId, request.getTitle());

		StudyPlanResponse response = aiStudyPlanService.generatePlanWithSubject(subjectId, request);

		log.info("AI plan with subject created — planId={}, sessions={}",
				response.getId(), response.getTotalSessions());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{planId}")
	public ResponseEntity<StudyPlanResponse> getPlanById(
			@PathVariable UUID planId) {

		log.info("Fetching study plan - planId={}", planId);

		StudyPlanResponse response = aiStudyPlanService.getPlanById(planId);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<StudyPlanResponse>> getUserPlans(
			@PathVariable UUID userId) {

		log.info("Fetching plans for user={}", userId);

		List<StudyPlanResponse> responses = aiStudyPlanService.getPlansByUserId(userId);

		return ResponseEntity.ok(responses);
	}

	@GetMapping("/subject/{subjectId}")
	public ResponseEntity<List<StudyPlanResponse>> getPlansBySubject(
			@PathVariable UUID subjectId) {

		return ResponseEntity.ok(
				aiStudyPlanService.getPlansBySubjectId(subjectId));
	}

	@PutMapping("/{planId}")
	public ResponseEntity<StudyPlanResponse> updatePlan(
			@PathVariable UUID planId,
			@Valid @RequestBody StudyPlanRequest request) {

		log.info("Updating study plan — planId={}, title='{}'", planId, request.getTitle());
		StudyPlanResponse response = aiStudyPlanService.updatePlan(planId, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{planId}")
	public ResponseEntity<Void> deletePlan(@PathVariable UUID planId) {
		log.info("Deleting study plan — planId={}", planId);
		aiStudyPlanService.deletePlan(planId);
		return ResponseEntity.noContent().build();
	}

	// ── Versioning & Rollback Endpoints ─────────────────────────────────

	@GetMapping("/{planId}/history")
	public ResponseEntity<List<StudyPlanHistoryResponse>> getPlanHistory(
			@PathVariable UUID planId) {

		log.info("Fetching version history — planId={}", planId);
		List<StudyPlanHistoryResponse> history = aiStudyPlanService.getPlanHistory(planId);
		return ResponseEntity.ok(history);
	}

	@GetMapping("/{planId}/history/{versionNumber}")
	public ResponseEntity<StudyPlanHistoryResponse> getPlanVersion(
			@PathVariable UUID planId,
			@PathVariable int versionNumber) {

		log.info("Fetching version snapshot — planId={}, version={}", planId, versionNumber);
		StudyPlanHistoryResponse version = aiStudyPlanService.getPlanVersion(planId, versionNumber);
		return ResponseEntity.ok(version);
	}

	@PostMapping("/{planId}/rollback/{versionNumber}")
	public ResponseEntity<StudyPlanResponse> rollbackPlan(
			@PathVariable UUID planId,
			@PathVariable int versionNumber) {

		log.info("Rolling back study plan — planId={}, targetVersion={}", planId, versionNumber);
		StudyPlanResponse response = aiStudyPlanService.rollbackPlan(planId, versionNumber);
		return ResponseEntity.ok(response);
	}
}