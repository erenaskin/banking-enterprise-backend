package com.banking.identity.service;

import com.banking.identity.dto.JwtResponse;
import com.banking.identity.dto.RefreshTokenRequest;
import com.banking.identity.entity.RefreshToken;
import com.banking.identity.entity.UserCredential;
import com.banking.identity.repository.RefreshTokenRepository;
import com.banking.identity.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.CredentialsExpiredException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_WithNewUser_ShouldCreateToken() {
        String username = "testuser";
        UserCredential user = new UserCredential();
        user.setUsername(username);

        when(userCredentialRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserCredential(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        RefreshToken token = refreshTokenService.createRefreshToken(username);

        assertNotNull(token);
        assertEquals(user, token.getUserCredential());
        assertNotNull(token.getToken());
    }

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewAccessToken() {
        String tokenStr = UUID.randomUUID().toString();
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken(tokenStr);

        UserCredential user = new UserCredential();
        user.setUsername("testuser");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenStr);
        refreshToken.setUserCredential(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(10000));

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateToken("testuser")).thenReturn("new-access-token");

        JwtResponse response = refreshTokenService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals(tokenStr, response.getRefreshToken());
    }

    @Test
    void verifyExpiration_WithExpiredToken_ShouldThrowException() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired-token");
        token.setExpiryDate(Instant.now().minusMillis(1000));

        assertThrows(CredentialsExpiredException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }
}