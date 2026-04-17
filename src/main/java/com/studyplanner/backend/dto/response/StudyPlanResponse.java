package com.studyplanner.backend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class 
StudyPlanResponse {

    private UUID id;
    private UUID userId;
    private UUID subjectId;
    private String subjectName;

    private String title;
    private String goal;
    private String difficulty;
    private String status;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dailyHours;

    private Integer totalSessions;
    private Integer completedSessions;
    private Integer progressPercent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<StudySessionResponse> sessions;
}
