package com.studyplanner.backend.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressRequest {

    private UUID sessionId;

    private Integer actualMinutes;

    private Short focusScore;

    private Boolean completed;

    private String remarks;
}