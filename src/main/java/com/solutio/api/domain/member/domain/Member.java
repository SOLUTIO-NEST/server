package com.solutio.api.domain.member.domain;

import com.solutio.api.global.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @Column(nullable = false, length = 9, unique = true, updatable = false)
    private String studentId;

    @Email
    @Column(name = "email", nullable = false, unique = true, updatable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 13)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String bojId;

    @Enumerated(EnumType.STRING)
    private MainLanguage mainLanguage;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    public static Member create(
        String studentId,
        String email,
        String password,
        String department,
        String name,
        String phoneNumber,
        String bojId,
        MainLanguage mainLanguage,
        PasswordEncoder passwordEncoder
    ) {
        return new Member(
            studentId,
            email,
            passwordEncoder.encode(password),
            department,
            name,
            phoneNumber,
            bojId,
            mainLanguage,
            Role.GUEST
        );
    }
}
