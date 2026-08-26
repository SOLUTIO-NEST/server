package com.solutio.api.domain.member.domain;

import com.solutio.api.domain.member.dto.request.MemberUpdateRequestDto;
import com.solutio.api.global.domain.BaseEntity;
import jakarta.persistence.*;
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

import java.util.Collection;
import java.util.Collections;

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

    @Enumerated(EnumType.STRING)
    private ClassLevel classLevel;

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
            ClassLevel classLevel
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
                classLevel,
                false
        );
    }

    public void updateMyInfo(MemberUpdateRequestDto requestDto) {
        this.name = requestDto.name();
        this.department = requestDto.department();
        this.phoneNumber = requestDto.phoneNumber();
        this.bojId = requestDto.bojId();
        this.mainLanguage = requestDto.mainLanguage();
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void reactivate(
            String email,
            String password,
            String department,
            String name,
            String phoneNumber,
            String bojId,
            MainLanguage mainLanguage,
            ClassLevel classLevel
    ) {
        this.email = email;
        this.password = password;
        this.department = department;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.bojId = bojId;
        this.mainLanguage = mainLanguage;
        this.role = Role.USER;
        this.classLevel = classLevel;
        this.isDeleted = false;
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
