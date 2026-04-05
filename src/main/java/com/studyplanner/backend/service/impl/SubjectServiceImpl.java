package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.dto.request.SubjectRequest;
import com.studyplanner.backend.dto.response.SubjectResponse;
import com.studyplanner.backend.entity.Subject;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.ResourceNotFoundException;
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

    @Override
    public SubjectResponse createSubject(UUID userId, SubjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Subject subject = Subject.builder()
                .user(user)
                .name(request.getName())
                .category(request.getCategory())
                .colorHex(request.getColorHex() != null ? request.getColorHex() : "#6B7280")
                .build();

        Subject saved = subjectRepository.save(subject);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));
        return mapToResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjectsByUser(UUID userId) {
        return subjectRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getActiveSubjectsByUser(UUID userId) {
        return subjectRepository.findByUserIdAndIsArchived(userId, false)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubjectResponse updateSubject(UUID subjectId, SubjectRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        subject.setName(request.getName());
        subject.setCategory(request.getCategory());
        if (request.getColorHex() != null) {
            subject.setColorHex(request.getColorHex());
        }

        Subject updated = subjectRepository.save(subject);
        return mapToResponse(updated);
    }

    @Override
    public SubjectResponse archiveSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));

        subject.setIsArchived(true);
        Subject archived = subjectRepository.save(subject);
        return mapToResponse(archived);
    }

    @Override
    public void deleteSubject(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with id: " + subjectId);
        }
        subjectRepository.deleteById(subjectId);
    }

    // ── Mapper ──────────────────────────────────────────────────
    private SubjectResponse mapToResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .category(subject.getCategory())
                .colorHex(subject.getColorHex())
                .isArchived(subject.getIsArchived())
                .createdAt(subject.getCreatedAt())
                .build();
    }
}
