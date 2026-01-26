package com.banking.identity.service;

import com.banking.identity.entity.UserCredential;
import com.banking.identity.repository.UserCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // JwtService içindeki @Value ile alınan secret alanını manuel set ediyoruz
        ReflectionTestUtils.setField(jwtService, "secret", "6eb5d4980dbe2d1986dbc05a859ae92aa57c82c6799012755856c6cd46bfd2db");
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        // Arrange
        String username = "testuser";
        UserCredential user = new UserCredential();
        user.setId(1);
        user.setUsername(username);

        when(userCredentialRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        String token = jwtService.generateToken(username);

        // Assert
        assertNotNull(token);
        assertDoesNotThrow(() -> jwtService.validateToken(token));
    }

    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        // Arrange
        String username = "testuser";

        // Act
        String token = jwtService.generateRefreshToken(username);

        // Assert
        assertNotNull(token);
        assertDoesNotThrow(() -> jwtService.validateToken(token));
    }

    @Test
    void validateToken_WithInvalidToken_ShouldThrowException() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act & Assert
        assertThrows(Exception.class, () -> jwtService.validateToken(invalidToken));
    }
}