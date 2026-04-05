package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.SubjectRequest;
import com.studyplanner.backend.dto.response.SubjectResponse;

import java.util.List;
import java.util.UUID;

public interface SubjectService {

    SubjectResponse createSubject(UUID userId, SubjectRequest request);

    SubjectResponse getSubjectById(UUID subjectId);

    List<SubjectResponse> getAllSubjectsByUser(UUID userId);

    List<SubjectResponse> getActiveSubjectsByUser(UUID userId);

    SubjectResponse updateSubject(UUID subjectId, SubjectRequest request);

    SubjectResponse archiveSubject(UUID subjectId);

    void deleteSubject(UUID subjectId);
}
