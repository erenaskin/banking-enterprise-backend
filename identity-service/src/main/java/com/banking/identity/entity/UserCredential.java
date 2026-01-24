package com.banking.identity.entity;

import com.banking.common.util.TCKN;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    @TCKN
    private String tckn;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthDate;
    private String username;
    private String email;
    private String password;
}