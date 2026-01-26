package com.banking.apigateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private final String SECRET = "6eb5d4980dbe2d1986dbc05a859ae92aa57c82c6799012755856c6cd46bfd2db";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void validateToken_WithValidToken_ShouldNotThrowException() {
        String token = createTestToken("testuser");
        assertDoesNotThrow(() -> jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.token.string";
        assertThrows(Exception.class, () -> jwtUtil.validateToken(invalidToken));
    }

    @Test
    void extractUserId_ShouldReturnUserId() {
        String token = createTestToken("testuser");
        String userId = jwtUtil.extractUserId(token);
        assertEquals("123", userId);
    }

    private String createTestToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "123");
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 10))
                .signWith(key)
                .compact();
    }
}