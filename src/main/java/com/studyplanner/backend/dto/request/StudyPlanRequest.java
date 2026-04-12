package com.studyplanner.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String goal;

    private UUID subjectId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(value = 1, message = "Daily hours must be at least 1")
    @Max(value = 16, message = "Daily hours must not exceed 16")
    @Builder.Default
    private Integer dailyHours = 2;

    @Builder.Default
    private String difficulty = "MEDIUM";

    /**
     * Optional list of topics to study.
     * If provided, sessions are generated from these topics.
     * If empty, generic day-wise sessions are created automatically.
     * Later this will be replaced by AI-generated topics.
     */
    private List<String> topics;
}
