package com.studyplanner.backend.repository;

import com.studyplanner.backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findByUserId(UUID userId);

    List<Subject> findByUserIdAndIsArchived(UUID userId, Boolean isArchived);

    boolean existsByUserIdAndName(UUID userId, String name);


}

