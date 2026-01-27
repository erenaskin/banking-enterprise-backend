package com.banking.identity.controller;

import com.banking.identity.config.EmbeddedRedisConfig;
import com.banking.identity.dto.AuthRequest;
import com.banking.identity.dto.JwtResponse;
import com.banking.identity.dto.RefreshTokenRequest;
import com.banking.identity.entity.UserCredential;
import com.banking.identity.service.AuthService;
import com.banking.identity.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Security filtrelerini devre dışı bırakmak için
@Import(EmbeddedRedisConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ShouldReturnSuccessMessage() throws Exception {
        UserCredential user = new UserCredential();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@test.com");

        when(authService.saveUser(any(UserCredential.class))).thenReturn("User added successfully");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("User added successfully"));
    }

    @Test
    void getToken_WithValidCredentials_ShouldReturnJwtResponse() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        when(authService.generateToken("testuser")).thenReturn(jwtResponse);

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void getToken_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("wrongpassword");

        // authenticationManager.authenticate() metodu BadCredentialsException fırlatacak
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid access: Authentication failed"));

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized()); // GlobalExceptionHandler sayesinde 401 döner
    }

    @Test
    void refreshToken_ShouldReturnJwtResponse() throws Exception {
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setToken("valid-refresh-token");

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("valid-refresh-token")
                .build();

        when(refreshTokenService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(jwtResponse);

        mockMvc.perform(post("/auth/refreshToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));
    }

    @Test
    void logout_ShouldReturnSuccessMessage() throws Exception {
        String token = "Bearer valid-token";

        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(authService).logout(token);
    }
}