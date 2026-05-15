package com.studyplanner.backend.mapper;

import com.studyplanner.backend.dto.request.UserRequest;
import com.studyplanner.backend.dto.response.UserResponse;
import com.studyplanner.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserRequest request);

    UserResponse toResponse(User user);

    void updateUserFromDto(UserRequest request,
                           @MappingTarget User user);
}