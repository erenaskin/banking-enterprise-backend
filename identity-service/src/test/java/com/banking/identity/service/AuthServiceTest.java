package com.banking.identity.service;

import com.banking.identity.dto.JwtResponse;
import com.banking.identity.entity.RefreshToken;
import com.banking.identity.entity.UserCredential;
import com.banking.identity.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    void saveUser_WithNewUser_ShouldSaveUser() {
        // Arrange
        UserCredential user = new UserCredential();
        user.setTckn("12345678901");
        user.setPassword("password");

        when(repository.findByTckn("12345678901")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        // Act
        String result = authService.saveUser(user);

        // Assert
        assertEquals("Kullanıcı başarıyla kaydedildi", result);
        verify(repository).save(user);
        assertEquals("encodedPassword", user.getPassword());
    }

    @Test
    void saveUser_WithExistingTckn_ShouldThrowException() {
        // Arrange
        UserCredential user = new UserCredential();
        user.setTckn("12345678901");

        when(repository.findByTckn("12345678901")).thenReturn(Optional.of(new UserCredential()));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.saveUser(user));
        assertEquals("User with this TCKN already exists", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void generateToken_ShouldReturnJwtResponse() {
        // Arrange
        String username = "testuser";
        String accessToken = "access-token";
        String refreshTokenStr = "refresh-token";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenStr);

        when(jwtService.generateToken(username)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(username)).thenReturn(refreshToken);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        JwtResponse response = authService.generateToken(username);

        // Assert
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshTokenStr, response.getRefreshToken());
        verify(valueOperations).set(accessToken, username, 15, TimeUnit.MINUTES);
    }
}