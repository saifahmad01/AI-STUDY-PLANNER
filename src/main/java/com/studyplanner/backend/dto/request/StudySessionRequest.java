package com.studyplanner.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySessionRequest {

    @NotNull(message = "Plan ID is required")
    private UUID planId;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    @NotBlank(message = "Topic is required")
    @Size(max = 200, message = "Topic cannot exceed 200 characters")
    private String topic;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private Boolean completed;

    private Integer actualDurationMinutes;

    @Min(value = 1, message = "Focus score must be between 1 and 10")
    @Max(value = 10, message = "Focus score must be between 1 and 10")
    private Short focusScore;

    private String notes;
}