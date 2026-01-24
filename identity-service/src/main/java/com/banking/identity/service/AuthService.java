package com.banking.identity.service;

import com.banking.identity.dto.JwtResponse;
import com.banking.identity.entity.UserCredential;
import com.banking.identity.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    @Autowired
    private UserCredentialRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String saveUser(UserCredential credential) {
        if (repository.findByTckn(credential.getTckn()).isPresent()) {
            throw new RuntimeException("User with this TCKN already exists");
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