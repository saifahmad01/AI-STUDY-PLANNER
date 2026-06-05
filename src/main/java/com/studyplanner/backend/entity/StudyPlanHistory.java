package com.studyplanner.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "study_plan_history", uniqueConstraints = {
        @UniqueConstraint(name = "uq_plan_version", columnNames = {"plan_id", "version_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StudyPlan plan;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Column(name = "difficulty", nullable = false, length = 20)
    private String difficulty;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "daily_hours", nullable = false)
    private Integer dailyHours;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions;

    @Column(name = "completed_sessions", nullable = false)
    private Integer completedSessions;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Builder.Default
    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt = LocalDateTime.now();

    @OneToMany(mappedBy = "planHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<StudySessionHistory> sessions = new ArrayList<>();
}
