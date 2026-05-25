package com.studyplanner.backend.mapper;

import com.studyplanner.backend.dto.request.StudyPlanRequest;
import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.entity.StudyPlan;
import com.studyplanner.backend.entity.Subject;
import com.studyplanner.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudyPlanMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    @Mapping(target = "difficulty", expression = "java(plan.getDifficulty().name())")
    @Mapping(target = "status", expression = "java(plan.getStatus().name())")
    @Mapping(target = "progressPercent", expression = "java(plan.getTotalSessions() != null && plan.getTotalSessions() > 0 ? "
            +
            "(plan.getCompletedSessions() * 100) / plan.getTotalSessions() : 0)")
    StudyPlanResponse toResponse(StudyPlan plan);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "goal", source = "request.goal")
    @Mapping(target = "difficulty", source = "request.difficulty")
    @Mapping(target = "startDate", source = "request.startDate")
    @Mapping(target = "endDate", source = "request.endDate")
    @Mapping(target = "dailyHours", source = "request.dailyHours")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "totalSessions", constant = "0")
    @Mapping(target = "completedSessions", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StudyPlan toEntity(StudyPlanRequest request, User user, Subject subject);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "goal", source = "request.goal")
    @Mapping(target = "difficulty", source = "request.difficulty")
    @Mapping(target = "startDate", source = "request.startDate")
    @Mapping(target = "endDate", source = "request.endDate")
    @Mapping(target = "dailyHours", source = "request.dailyHours")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "totalSessions", ignore = true)
    @Mapping(target = "completedSessions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(StudyPlanRequest request, Subject subject, @MappingTarget StudyPlan plan);

    default StudyPlan.Difficulty mapDifficulty(String difficulty) {
        if (difficulty == null) {
            return StudyPlan.Difficulty.MEDIUM;
        }
        try {
            return StudyPlan.Difficulty.valueOf(difficulty.toUpperCase());
        } catch (Exception e) {
            return StudyPlan.Difficulty.MEDIUM;
        }
    }
}