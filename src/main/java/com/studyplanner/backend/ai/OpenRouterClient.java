package com.studyplanner.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenRouterClient {

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sends a prompt to OpenRouter and returns the AI's text response.
     */
    public String chat(String systemPrompt, String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "https://studyplanner.app"); // OpenRouter requires this
            headers.set("X-Title", "StudyPlanner AI");

            Map<String, Object> body = new HashMap<>();
            body.put("model", "openai/gpt-4o-mini"); // cost-effective model; change as needed
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user",   "content", userPrompt)
            ));
            body.put("temperature", 0.7);
            body.put("max_tokens", 1500);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root
                    .path("choices").get(0)
                    .path("message").path("content")
                    .asText();

            log.debug("OpenRouter response: {}", content);
            return content;

        } catch (Exception e) {
            log.error("OpenRouter API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable. Please try again.", e);
        }
    }
}