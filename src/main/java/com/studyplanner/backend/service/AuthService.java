package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.LoginRequest;
import com.studyplanner.backend.dto.request.RegisterRequest;
import com.studyplanner.backend.dto.request.TokenRefreshRequest;
import com.studyplanner.backend.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(TokenRefreshRequest request);

    void logout(String refreshToken);
}