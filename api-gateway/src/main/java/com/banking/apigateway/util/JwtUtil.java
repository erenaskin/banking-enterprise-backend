package com.banking.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public void validateToken(final String token) {
        Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token);
    }

    public String extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // Token oluşturulurken user id "userId" claim'ine eklenmiş olmalı.
        // Eğer subject olarak eklendiyse claims.getSubject() kullanılabilir.
        // Şimdilik subject'in username olduğunu varsayarak, token oluşturma mantığına göre burayı güncellemeliyiz.
        // IdentityService'deki generateToken metoduna bakarak userId'nin nereye konduğunu teyit etmeliyiz.
        // Geçici olarak subject'i dönüyorum, ancak IdentityService'i kontrol edip düzelteceğim.
        return claims.get("userId", String.class);
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}