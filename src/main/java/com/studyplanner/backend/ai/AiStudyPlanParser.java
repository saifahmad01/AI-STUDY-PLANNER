package com.studyplanner.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the AI's raw JSON text response into a {@link StudyPlanRequest}.
 *
 * The AI is expected to return:
 *   { "title", "goal", "difficulty", "dailyHours", "topics": [...] }
 *
 * NOTE: startDate and endDate are NOT parsed from AI — they are always
 * taken from the original user request (set back in AiStudyPlanServiceImpl).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiStudyPlanParser {

    private final ObjectMapper objectMapper;

    public StudyPlanRequest parse(String aiResponse) {
        try {
            // Strip markdown code fences if model adds them despite instructions
            String cleaned = aiResponse
                    .replaceAll("(?s)```json", "")
                    .replaceAll("(?s)```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);

            StudyPlanRequest request = new StudyPlanRequest();

            // ── Fields from AI ──────────────────────────────────────
            request.setTitle(node.path("title").asText());
            request.setGoal(node.path("goal").asText());
            request.setDifficulty(node.path("difficulty").asText("MEDIUM"));
            request.setDailyHours(node.path("dailyHours").asInt(2));

            // ── Topics ──────────────────────────────────────────────
            List<String> topics = new ArrayList<>();
            JsonNode topicsNode = node.path("topics");
            if (topicsNode.isArray()) {
                topicsNode.forEach(t -> topics.add(t.asText()));
            }
            request.setTopics(topics);

            // startDate and endDate are NOT set here —
            // they will be filled by the service from the user's original request.

            log.info("AI response parsed: title='{}' | {} topics | difficulty={}",
                    request.getTitle(), topics.size(), request.getDifficulty());

            return request;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", aiResponse);
            throw new BadRequestException("AI returned an invalid plan format. Please try again.");
        }
    }
}