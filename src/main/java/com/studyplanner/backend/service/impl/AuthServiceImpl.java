package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.dto.request.LoginRequest;
import com.studyplanner.backend.dto.request.RegisterRequest;
import com.studyplanner.backend.dto.request.TokenRefreshRequest;
import com.studyplanner.backend.dto.response.AuthResponse;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.DuplicateResourceException;
import com.studyplanner.backend.repository.UserRepository;
import com.studyplanner.backend.security.JwtUtil;
import com.studyplanner.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .message("User registered successfully")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .message("Login successful")
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new BadCredentialsException("Token already expired");
        }

        String type = jwtUtil.extractClaim(refreshToken, claims -> claims.get("type", String.class));
        if (!"refresh".equals(type)) {
            throw new BadCredentialsException("Invalid token type provided for logout");
        }


    }

    @Override
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        if (jwtUtil.isTokenExpired(requestRefreshToken)) {
            throw new BadCredentialsException("Refresh token is expired. Please login again.");
        }

        String email = jwtUtil.extractUsername(requestRefreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for token"));

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .name(user.getName())
                .message("Token refreshed successfully")
                .build();
    }
}