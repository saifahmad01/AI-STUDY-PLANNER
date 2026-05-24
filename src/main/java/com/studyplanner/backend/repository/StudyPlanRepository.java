package com.studyplanner.backend.repository;

import com.studyplanner.backend.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, UUID> {

    List<StudyPlan> findByUserId(UUID userId);

    List<StudyPlan> findByUserIdAndStatus(UUID userId, StudyPlan.Status status);

    List<StudyPlan> findBySubjectId(UUID subjectId);

    Optional<StudyPlan>  findByUser_IdAndTitle(UUID userid ,String title);

    long countByUserIdAndStatus(UUID userId, StudyPlan.Status status);
}
