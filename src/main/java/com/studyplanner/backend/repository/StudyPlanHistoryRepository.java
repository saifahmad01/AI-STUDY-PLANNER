package com.studyplanner.backend.repository;

import com.studyplanner.backend.entity.StudyPlanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyPlanHistoryRepository extends JpaRepository<StudyPlanHistory, UUID> {

    List<StudyPlanHistory> findByPlanIdOrderByVersionNumberDesc(UUID planId);

    Optional<StudyPlanHistory> findByPlanIdAndVersionNumber(UUID planId, int versionNumber);

    Optional<StudyPlanHistory> findTopByPlanIdOrderByVersionNumberDesc(UUID planId);
}
