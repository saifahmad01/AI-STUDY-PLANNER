package com.studyplanner.backend.mapper;

import com.studyplanner.backend.dto.response.StudySessionResponse;
import com.studyplanner.backend.entity.StudySession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudySessionMapper {

    @Mapping(target = "planId", source = "plan.id")
    StudySessionResponse toResponse(StudySession session);
}