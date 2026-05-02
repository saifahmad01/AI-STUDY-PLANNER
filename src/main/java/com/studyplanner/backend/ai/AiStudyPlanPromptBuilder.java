package com.studyplanner.backend.ai;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import org.springframework.stereotype.Component;

/**
 * Builds system and user prompts for OpenRouter AI
 * based on a {@link StudyPlanRequest}.
 *
 * Field mapping from StudyPlanRequest:
 *   title       → subject to study
 *   goal        → learning goal
 *   startDate   → plan start (user-provided, passed to AI for context)
 *   endDate     → plan end   (user-provided, passed to AI for context)
 *   dailyHours  → hours available per day
 *   difficulty  → EASY | MEDIUM | HARD
 *   topics      → optional focus areas (comma-separated in prompt)
 */
@Component
public class AiStudyPlanPromptBuilder {

    /**
     * System prompt telling the AI exactly what JSON schema to return.
     * Dates are NOT in the schema — the user always provides them.
     */
    public String buildSystemPrompt() {
        return """
            You are an expert study coach and curriculum designer.
            Your job is to create a structured, realistic study plan based on the user's input.
            
            IMPORTANT: Always respond with ONLY valid JSON. No explanation, no markdown, no code blocks.
            The JSON must strictly follow this schema:
            {
              "title": "string",
              "goal": "string",
              "difficulty": "EASY|MEDIUM|HARD",
              "dailyHours": number,
              "topics": ["topic1", "topic2", ...]
            }
            
            Rules:
            - topics should be 5–15 specific, actionable items that fit within the given date range
            - dailyHours must match the user's stated availability (1–16)
            - difficulty must be exactly EASY, MEDIUM, or HARD (uppercase)
            - Do NOT include startDate or endDate in your response; they are provided by the user
        """;
    }

    /**
     * User prompt built from the {@link StudyPlanRequest}.
     */
    public String buildUserPrompt(StudyPlanRequest request) {
        String focusAreas = (request.getTopics() != null && !request.getTopics().isEmpty())
                ? String.join(", ", request.getTopics())
                : "none";

        return String.format("""
            Create a study plan for me with the following details:
            
            Subject / Title : %s
            Goal            : %s
            Start date      : %s
            End date        : %s
            Hours per day   : %d
            Difficulty      : %s
            Focus areas     : %s
            
            Generate a realistic plan with a clear title and 5–15 specific,
            actionable topics that fit within the start-to-end date range.
            Remember: respond with ONLY the JSON object, no extra text.
            """,
                request.getTitle(),
                request.getGoal() != null ? request.getGoal() : "Not specified",
                request.getStartDate(),
                request.getEndDate(),
                request.getDailyHours(),
                request.getDifficulty(),
                focusAreas
        );
    }
}