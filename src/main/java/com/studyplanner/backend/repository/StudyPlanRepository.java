package com.studyplanner.backend.repository;

import com.studyplanner.backend.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, UUID> {

    List<StudyPlan> findByUserId(UUID userId);

    List<StudyPlan> findByUserIdAndStatus(UUID userId, StudyPlan.Status status);

    List<StudyPlan> findBySubjectId(UUID subjectId);

    long countByUserIdAndStatus(UUID userId, StudyPlan.Status status);
}
