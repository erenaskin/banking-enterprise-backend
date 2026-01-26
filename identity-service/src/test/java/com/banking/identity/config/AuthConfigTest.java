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
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc // MockMvc'yi kullanmak için
class AuthConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc; // Güvenlik kurallarını test etmek için

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
        assertNotNull(((DaoAuthenticationProvider) authenticationProvider).getUserDetailsService());
        assertNotNull(((DaoAuthenticationProvider) authenticationProvider).getPasswordEncoder());
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

    // Güvenlik kurallarını test eden metotlar
    @Test
    void unsecuredEndpointsShouldPermitAll() throws Exception {
        // /auth/register
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()); // veya 400 Bad Request, çünkü body boş ama erişim izni var

        // /auth/token
        mockMvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()); // BadCredentialsException fırlatılacak, bu da 401'e dönüşebilir

        // /auth/validate (varsayılan olarak permitAll değil, AuthController'da yok)
        // Eğer AuthController'da /auth/validate yoksa bu test başarısız olur veya 404 döner.
        // Varsayılan olarak permitAll olan endpoint'leri test edelim.
        // AuthController'da /auth/validate yok, bu yüzden bu endpoint'i test etmek yerine
        // mevcut permitAll olanları test edelim.
        // /auth/logout da permitAll değil, authenticated olması lazım.

        // AuthController'daki permitAll endpoint'leri: /auth/register, /auth/token
        // /auth/logout için bir token gerekecek, bu yüzden onu burada test etmek zor.
        // Sadece permitAll olanları test edelim.
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