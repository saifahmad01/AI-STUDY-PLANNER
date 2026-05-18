package com.studyplanner.backend.mapper;

import com.studyplanner.backend.dto.response.StudyPlanResponse;
import com.studyplanner.backend.entity.StudyPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudyPlanMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "subjectId", source = "subject.id")
    @Mapping(target = "subjectName", source = "subject.name")
    @Mapping(target = "difficulty", expression = "java(plan.getDifficulty().name())")
    @Mapping(target = "status", expression = "java(plan.getStatus().name())")
    @Mapping(target = "progressPercent",
            expression = "java(plan.getTotalSessions() != null && plan.getTotalSessions() > 0 ? " +
                    "(plan.getCompletedSessions() * 100) / plan.getTotalSessions() : 0)")
    StudyPlanResponse toResponse(StudyPlan plan);
}