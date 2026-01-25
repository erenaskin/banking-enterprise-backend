package com.banking.identity.service;

import com.banking.identity.entity.UserCredential;
import com.banking.identity.repository.UserCredentialRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private final UserCredentialRepository userCredentialRepository;

    @Value("${jwt.secret}")
    private String secret;

    public JwtService(UserCredentialRepository userCredentialRepository) {
        this.userCredentialRepository = userCredentialRepository;
    }

    public void validateToken(final String token) {
        Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token);
    }

    public String generateToken(String userName) {
        Map<String, Object> claims = new HashMap<>();
        Optional<UserCredential> user = userCredentialRepository.findByUsername(userName);
        user.ifPresent(userCredential -> claims.put("userId", String.valueOf(userCredential.getId())));
        return createToken(claims, userName, 1000 * 60 * 15); // 15 minutes
    }

    public String generateRefreshToken(String userName) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userName, 1000 * 60 * 60 * 24 * 7); // 7 days
    }

    private String createToken(Map<String, Object> claims, String userName, long expirationTime) {
        return Jwts.builder()
                .claims(claims)
                .subject(userName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignKey())
                .compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}