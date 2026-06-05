package com.studyplanner.backend.repository;

import com.studyplanner.backend.entity.StudySessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudySessionHistoryRepository extends JpaRepository<StudySessionHistory, UUID> {

    List<StudySessionHistory> findByPlanHistoryId(UUID planHistoryId);
}
