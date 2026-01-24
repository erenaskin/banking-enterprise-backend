package com.banking.identity.service;

import com.banking.identity.dto.JwtResponse;
import com.banking.identity.dto.RefreshTokenRequest;
import com.banking.identity.entity.RefreshToken;
import com.banking.identity.entity.UserCredential;
import com.banking.identity.repository.RefreshTokenRepository;
import com.banking.identity.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private JwtService jwtService;

    public RefreshToken createRefreshToken(String username) {
        UserCredential user = userCredentialRepository.findByUsername(username).get();
        
        // Kullanıcının zaten bir refresh token'ı var mı kontrol et
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserCredential(user);
        
        RefreshToken refreshToken;
        if (existingToken.isPresent()) {
            // Varsa güncelle
            refreshToken = existingToken.get();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(1000L * 60 * 60 * 24 * 7)); // 7 days
        } else {
            // Yoksa yeni oluştur
            refreshToken = RefreshToken.builder()
                    .userCredential(user)
                    .token(UUID.randomUUID().toString())
                    .expiryDate(Instant.now().plusMillis(1000L * 60 * 60 * 24 * 7)) // 7 days
                    .build();
        }

        return refreshTokenRepository.save(refreshToken);
    }

    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return refreshTokenRepository.findByToken(refreshTokenRequest.getToken())
                .map(this::verifyExpiration)
                .map(RefreshToken::getUserCredential)
                .map(userCredential -> {
                    String accessToken = jwtService.generateToken(userCredential.getUsername());
                    return JwtResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshTokenRequest.getToken())
                            .build();
                }).orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException(token.getToken() + " Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}