package com.studyplanner.backend.repository;

import com.studyplanner.backend.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    List<StudySession> findByPlanId(UUID planId);

    List<StudySession> findByPlanIdAndCompleted(UUID planId, boolean completed);

    List<StudySession> findByPlanIdAndScheduledDate(UUID planId, LocalDate scheduledDate);

    List<StudySession> findByScheduledDateBetween(LocalDate start, LocalDate end);

    long countByPlanIdAndCompleted(UUID planId, boolean completed);
}
