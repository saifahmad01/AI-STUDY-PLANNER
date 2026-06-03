package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.dto.request.SubjectRequest;
import com.studyplanner.backend.dto.response.SubjectResponse;
import com.studyplanner.backend.entity.Subject;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.mapper.SubjectMapper;
import com.studyplanner.backend.repository.SubjectRepository;
import com.studyplanner.backend.repository.UserRepository;
import com.studyplanner.backend.service.SubjectService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final SubjectMapper subjectMapper;

    @Override
    public SubjectResponse createSubject(UUID userId, SubjectRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId));

        if (subjectRepository.existsByUserIdAndNameIgnoreCase(
                userId,
                request.getName().trim())) {

            throw new IllegalArgumentException(
                    "Subject '" + request.getName() + "' already exists");
        }

        Subject subject = subjectMapper.toEntity(request);

        subject.setUser(user);

        if (subject.getColorHex() == null) {
            subject.setColorHex("#6B7280");
        }

        Subject saved = subjectRepository.save(subject);

        return subjectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));
        return subjectMapper.toResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjectsByUser(UUID userId) {

        return subjectRepository.findByUserId(userId)
                .stream()
                .map(subjectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getActiveSubjectsByUser(UUID userId) {
        return subjectRepository.findByUserIdAndIsArchived(userId, false)
                .stream()
                .map(subjectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubjectResponse updateSubject(UUID subjectId,
                                         SubjectRequest request) {

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subject not found with id: " + subjectId));

        subjectMapper.updateSubjectFromDto(request, subject);

        Subject updated = subjectRepository.save(subject);

        return subjectMapper.toResponse(updated);
    }

    @Override
    public SubjectResponse archiveSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        subject.setIsArchived(true);
        Subject archived = subjectRepository.save(subject);
        return subjectMapper.toResponse(archived);
    }

    @Override
    public void deleteSubject(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        subjectRepository.deleteById(subjectId);
    }

}
