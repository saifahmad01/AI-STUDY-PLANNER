package com.studyplanner.backend.repository;

import com.studyplanner.backend.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressRepository extends JpaRepository<Progress, UUID> {

    List<Progress> findByUserId(UUID userId);

    List<Progress> findByUserIdAndStudyDate(UUID userId,
                                            LocalDate studyDate);

    Optional<Progress> findByStudySessionId(UUID sessionId);

    long countByUserIdAndCompleted(UUID userId,
                                   Boolean completed);
}