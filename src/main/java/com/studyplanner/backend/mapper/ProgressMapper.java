package com.studyplanner.backend.mapper;

import com.studyplanner.backend.dto.request.ProgressRequest;
import com.studyplanner.backend.dto.request.UserRequest;
import com.studyplanner.backend.dto.response.ProgressResponse;
import com.studyplanner.backend.entity.Progress;
import com.studyplanner.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "sessionId", source = "studySession.id")
    @Mapping(target = "topic", source = "studySession.topic")
    ProgressResponse toResponse(Progress progress);
    Progress toEntity(ProgressRequest request);
}