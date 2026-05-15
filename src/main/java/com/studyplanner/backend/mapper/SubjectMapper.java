package com.studyplanner.backend.mapper;

import com.studyplanner.backend.dto.request.SubjectRequest;
import com.studyplanner.backend.dto.response.SubjectResponse;
import com.studyplanner.backend.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubjectMapper {

    Subject toEntity(SubjectRequest request);

    SubjectResponse toResponse(Subject subject);

    void updateSubjectFromDto(SubjectRequest request,
                              @MappingTarget Subject subject);
}