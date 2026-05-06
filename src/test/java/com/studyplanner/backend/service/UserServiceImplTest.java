package com.studyplanner.backend.service;

import com.studyplanner.backend.dto.request.UserRequest;
import com.studyplanner.backend.dto.response.UserResponse;
import com.studyplanner.backend.entity.User;
import com.studyplanner.backend.exception.ResourceNotFoundException;
import com.studyplanner.backend.repository.UserRepository;
import com.studyplanner.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateUserSuccessfully() {

        // Arrange
        UserRequest request = UserRequest.builder()
                .name("Saif")
                .email("saif@gmail.com")
                .password("123456")
                .build();

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("Saif")
                .email("saif@gmail.com")
                .password("123456")
                .build();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // Act
        UserResponse response = userService.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("Saif", response.getName());
        assertEquals("saif@gmail.com", response.getEmail());

        verify(userRepository).save(any(User.class));
    }
}