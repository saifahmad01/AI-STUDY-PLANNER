package com.studyplanner.backend.controller;

import com.studyplanner.backend.ai.OpenRouterClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final OpenRouterClient client;

    @GetMapping
    public String testAI() {
        return client.chat(
                "You are a helpful assistant",
                "Say hello in one short sentence"
        );
    }
}