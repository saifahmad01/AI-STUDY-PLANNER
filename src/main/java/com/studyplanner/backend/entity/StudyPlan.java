package com.studyplanner.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_plans",
        uniqueConstraints = {},
        indexes = {}
)
@Check(constraints = "end_date > start_date")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Subject subject;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "daily_hours", nullable = false)
    private Integer dailyHours = 2;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Builder.Default
    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions = 0;

    @Builder.Default
    @Column(name = "completed_sessions", nullable = false)
    private Integer completedSessions = 0;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Difficulty { EASY, MEDIUM, HARD }
    public enum Status     { ACTIVE, PAUSED, COMPLETED, ARCHIVED }
}
