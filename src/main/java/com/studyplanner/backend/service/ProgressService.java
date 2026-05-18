package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.ProgressRequest;
import com.studyplanner.backend.dto.response.ProgressResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProgressService {

    ProgressResponse trackProgress(UUID userId,
                                   ProgressRequest request);

    List<ProgressResponse> getUserProgress(UUID userId);

    List<ProgressResponse> getDailyProgress(UUID userId,
                                            LocalDate date);

    long getCompletedSessions(UUID userId);
}