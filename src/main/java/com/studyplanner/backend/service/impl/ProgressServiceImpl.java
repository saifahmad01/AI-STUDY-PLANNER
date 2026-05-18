package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.dto.request.ProgressRequest;
import com.studyplanner.backend.dto.response.ProgressResponse;
import com.studyplanner.backend.entity.Progress;
import com.studyplanner.backend.entity.StudySession;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.mapper.ProgressMapper;
import com.studyplanner.backend.repository.ProgressRepository;
import com.studyplanner.backend.repository.StudySessionRepository;
import com.studyplanner.backend.repository.UserRepository;
import com.studyplanner.backend.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgressServiceImpl implements ProgressService {

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final StudySessionRepository studySessionRepository;
    private final ProgressMapper progressMapper;

    @Override
    public ProgressResponse trackProgress(UUID userId,
                                          ProgressRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        StudySession session = studySessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Study session not found"));

        double completionPercentage =
                ((double) request.getActualMinutes()
                        / session.getDurationMinutes()) * 100;

        Progress progress = progressMapper.toEntity(request);

        Progress savedProgress = progressRepository.save(progress);

        session.setCompleted(request.getCompleted());
        session.setActualDurationMinutes(request.getActualMinutes());
        session.setFocusScore(request.getFocusScore());

        studySessionRepository.save(session);

        return progressMapper.toResponse(savedProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressResponse> getUserProgress(UUID userId) {

        return progressRepository.findByUserId(userId)
                .stream()
                .map(progressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressResponse> getDailyProgress(UUID userId,
                                                   LocalDate date) {

        return progressRepository.findByUserIdAndStudyDate(userId, date)
                .stream()
                .map(progressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getCompletedSessions(UUID userId) {
        return progressRepository.countByUserIdAndCompleted(userId, true);
    }
}