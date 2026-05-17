package com.studyplanner.backend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressResponse {

    private UUID id;

    private UUID userId;

    private UUID sessionId;

    private String topic;

    private LocalDate studyDate;

    private Integer plannedMinutes;

    private Integer actualMinutes;

    private Double completionPercentage;

    private Short focusScore;

    private Boolean completed;

    private String remarks;
}