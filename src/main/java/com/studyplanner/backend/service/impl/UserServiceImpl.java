package com.studyplanner.backend.service.impl;

import com.studyplanner.backend.dto.request.UserRequest;
import com.studyplanner.backend.dto.response.UserResponse;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.mapper.UserMapper;
import com.studyplanner.backend.repository.UserRepository;
import com.studyplanner.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // ── Create

    @Override
    public UserResponse createUser(UserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email is already registered: " + request.getEmail()
            );
        }

        User user = userMapper.toEntity(request);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    // ── Read by ID

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        ));

        return userMapper.toResponse(user);
    }

    // ── Read by Email

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: " + email
                        ));

        return userMapper.toResponse(user);
    }

    // ── Read All

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Update

    @Override
    public UserResponse updateUser(UUID userId, UserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        ));

        userMapper.updateUserFromDto(request, user);

        User updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }

    // ── Delete

    @Override
    public void deleteUser(UUID userId) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }

        userRepository.deleteById(userId);
    }
}