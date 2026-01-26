package com.banking.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AuthConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void beansShouldBeLoaded() {
        assertNotNull(context.getBean(PasswordEncoder.class));
        assertNotNull(context.getBean(AuthenticationProvider.class));
        assertNotNull(context.getBean(AuthenticationManager.class));
        assertNotNull(context.getBean("securityFilterChain", SecurityFilterChain.class));
    }
}