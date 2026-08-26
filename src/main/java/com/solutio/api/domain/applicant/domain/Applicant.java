package com.solutio.api.domain.applicant.domain;

import com.solutio.api.domain.applicant.dto.request.ApplicantUpdateClassLevelRequestDto;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.member.dto.request.MemberUpdateRequestDto;
import com.solutio.api.global.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Applicant extends BaseEntity implements UserDetails {

    @Id
    @Column(nullable = false, length = 9, unique = true, updatable = false)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id")
    private Recruitment recruitment;

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

    @Column(length = 1024)
    private String applyReason;

    @Enumerated(EnumType.STRING)
    private ClassLevel classLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassStatus passStatus;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @Override
    public String getUsername() {
        return studentId;
    }

    public boolean isLoginPeriodExpired() {
        if (this.recruitment == null) {
            return true;
        }
        return this.recruitment.isResultInquiryPeriodExpired();
    }

    public static Applicant create(
            String studentId,
            Recruitment recruitment,
            String email,
            String password,
            String department,
            String name,
            String phoneNumber,
            String bojId,
            MainLanguage mainLanguage,
            String applyReason,
            PasswordEncoder passwordEncoder
    ) {
        return new Applicant(
                studentId,
                recruitment,
                email,
                passwordEncoder.encode(password),
                department,
                name,
                phoneNumber,
                bojId,
                mainLanguage,
                applyReason,
                ClassLevel.UNASSIGNED,
                PassStatus.PENDING
        );
    }

    public void approve() {
        this.passStatus = PassStatus.APPROVED;
    }

    public void reject() {
        this.passStatus = PassStatus.REJECTED;
    }

    public boolean isApproved() {
        return this.passStatus == PassStatus.APPROVED;
    }

    public void updateClassLevel(ApplicantUpdateClassLevelRequestDto requestDto) {
        this.classLevel = requestDto.getClassLevel() == null ? ClassLevel.UNASSIGNED : requestDto.getClassLevel();
    }

    public void updateMyInfo(MemberUpdateRequestDto requestDto) {
        this.name = requestDto.name();
        this.department = requestDto.department();
        this.phoneNumber = requestDto.phoneNumber();
        this.bojId = requestDto.bojId();
        this.mainLanguage = requestDto.mainLanguage();
    }
}
