package com.banking.identity.repository;

import com.banking.identity.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Integer> {
    Optional<UserCredential> findByUsername(String username);
    Optional<UserCredential> findByTckn(String tckn);
}