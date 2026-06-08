
package com.studyplanner.backend.entity;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.UUID;

@Entity

@Table(name = "study_session_history")

@Data

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class StudySessionHistory {

    @Id

    @GeneratedValue(strategy = GenerationType.AUTO)

    @Column(name = "id", updatable = false, nullable = false)

    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "plan_history_id", nullable = false)

    @ToString.Exclude

    @EqualsAndHashCode.Exclude

    private StudyPlanHistory planHistory;

    @Column(name = "original_session_id")

    private UUID originalSessionId;

    @Column(name = "scheduled_date", nullable = false)

    private LocalDate scheduledDate;

    @Column(name = "topic", nullable = false, length = 200)

    private String topic;

    @Column(name = "duration_minutes", nullable = false)

    private Integer durationMinutes;

    @Builder.Default

    @Column(name = "completed", nullable = false)

    private Boolean completed = false;

    @Column(name = "actual_duration_minutes")

    private Integer actualDurationMinutes;

    @Column(name = "focus_score")

    private Short focusScore;

    @Column(name = "notes", columnDefinition = "TEXT")

    private String notes;

    @Column(name = "completed_at")

    private LocalDateTime completedAt;

}
