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
public class StudyPlanHistoryResponse {

    private UUID id;
    private UUID planId;
    private Integer versionNumber;

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

    private String changeReason;
    private LocalDateTime snapshotAt;

    private List<StudySessionResponse> sessions;
}
