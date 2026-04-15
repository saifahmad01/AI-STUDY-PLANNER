package com.studyplanner.backend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySessionResponse {

    private UUID id;
    private UUID planId;

    private LocalDate scheduledDate;
    private String topic;
    private Integer durationMinutes;

    private Boolean completed;
    private Integer actualDurationMinutes;
    private Short focusScore;
    private String notes;
    private LocalDateTime completedAt;
}
