package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.UserRequest;
import com.studyplanner.backend.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse getUserById(UUID userId);

    UserResponse getUserByEmail(String email);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(UUID userId, UserRequest request);

    void deleteUser(UUID userId);
}
