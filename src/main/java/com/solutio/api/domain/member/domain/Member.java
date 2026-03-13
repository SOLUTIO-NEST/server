package com.solutio.api.domain.member.domain;

import com.solutio.api.global.domain.BaseEntity;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
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
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Member extends BaseEntity implements UserDetails {

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

    private LevelClass levelClass;

    @Column(nullable = false)
    private Boolean isDeleted;

    public static Member createFromApplicant(
        String studentId,
        String email,
        String password,
        String department,
        String name,
        String phoneNumber,
        String bojId,
        MainLanguage mainLanguage,
        LevelClass levelClass
    ) {
        return new Member(
            studentId,
            email,
            password,
            department,
            name,
            phoneNumber,
            bojId,
            mainLanguage,
            Role.USER,
            levelClass,
            false
        );
    }

    public void isPasswordMatching(String rawPassword, PasswordEncoder passwordEncoder) {
        if (!passwordEncoder.matches(rawPassword, this.password)) {
            throw new GeneralException(Status.INVALID_PASSWORD);
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(getRole().name()));
    }

    @Override
    public String getUsername() {
        return studentId;
    }
}
