package com.banking.identity.repository;

import com.banking.identity.entity.RefreshToken;
import com.banking.identity.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserCredential(UserCredential userCredential);
}