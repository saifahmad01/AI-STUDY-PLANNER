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

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {

        // Arrange
        UserRequest request = UserRequest.builder()
                .name("Saif")
                .email("saif@gmail.com")
                .password("123")
                .build();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.createUser(request)
                );

        assertEquals(
                "Email is already registered: saif@gmail.com",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnUserById() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Saif")
                .email("saif@gmail.com")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getUserById(userId);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Saif", response.getName());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {

        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> userService.getUserById(userId)
                );

        assertEquals(
                "User not found with id: " + userId,
                exception.getMessage()
        );
    }
}