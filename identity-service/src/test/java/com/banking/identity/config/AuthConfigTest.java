package com.banking.identity.config;

import com.banking.identity.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void passwordEncoderBeanShouldBeLoaded() {
        assertNotNull(context.getBean(PasswordEncoder.class));
        assertTrue(context.getBean(PasswordEncoder.class) instanceof BCryptPasswordEncoder);
    }

    @Test
    void authenticationProviderBeanShouldBeLoaded() {
        AuthenticationProvider authenticationProvider = context.getBean(AuthenticationProvider.class);
        assertNotNull(authenticationProvider);
        assertTrue(authenticationProvider instanceof DaoAuthenticationProvider);
    }

    @Test
    void authenticationManagerBeanShouldBeLoaded() {
        assertNotNull(context.getBean(AuthenticationManager.class));
    }

    @Test
    void securityFilterChainsShouldBeLoaded() {
        assertNotNull(context.getBean("securityFilterChain", SecurityFilterChain.class));
        assertNotNull(context.getBean("actuatorFilterChain", SecurityFilterChain.class));
    }

    @Test
    void userDetailsServiceBeanShouldBeLoaded() {
        assertNotNull(context.getBean(UserDetailsService.class));
        assertTrue(context.getBean(UserDetailsService.class) instanceof CustomUserDetailsService);
    }

    @Test
    void unsecuredEndpointsShouldPermitAll() throws Exception {
        // First registration - should succeed (200 OK)
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\", \"password\":\"password\", \"tckn\":\"10000000146\", \"email\":\"test@test.com\", \"firstName\":\"Ali\", \"lastName\":\"Veli\"}"))
                .andExpect(status().isOk());

        // Second registration with same TCKN - should fail with 409 Conflict
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser2\", \"password\":\"password\", \"tckn\":\"10000000146\", \"email\":\"test2@test.com\", \"firstName\":\"Ahmet\", \"lastName\":\"Yılmaz\"}"))
                .andExpect(status().isConflict()); // GlobalExceptionHandler handles DuplicateKeyException

        // /auth/token - Boş body ile 401 Unauthorized beklenir
        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securedEndpointsShouldRequireAuthentication() throws Exception {
        // Rastgele bir korumalı endpoint'e erişim denemesi
        mockMvc.perform(get("/some/secured/path"))
                .andExpect(status().isUnauthorized()); // Kimlik doğrulaması gerektirdiği için 401 döner
    }

    @Test
    void actuatorEndpointsShouldPermitAll() throws Exception {
        // Actuator health endpoint
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        // Actuator info endpoint
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }
}