package com.banking.identity.service;

import com.banking.identity.dto.JwtResponse;
import com.banking.identity.entity.UserCredential;
import com.banking.identity.repository.UserCredentialRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserCredentialRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.redisTemplate = redisTemplate;
    }

    public String saveUser(UserCredential credential) {
        if (repository.findByTckn(credential.getTckn()).isPresent()) {
            throw new DuplicateKeyException("User with this TCKN already exists");
        }
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        repository.save(credential);
        return "Kullanıcı başarıyla kaydedildi";
    }

    public JwtResponse generateToken(String username) {
        String accessToken = jwtService.generateToken(username);
        String refreshToken = refreshTokenService.createRefreshToken(username).getToken();
        redisTemplate.opsForValue().set(accessToken, username, 15, TimeUnit.MINUTES);
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(String token) {
        String jwt = token.substring(7);
        redisTemplate.opsForValue().set("blacklist:" + jwt, "", 15, TimeUnit.MINUTES);
    }
}