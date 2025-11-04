package com.solutio.api.domain.applicant.domain;

import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.global.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Applicant extends BaseEntity {

    @Id
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@(kgu\\.ac\\.kr|kyonggi\\.ac\\.kr)$",
        message = "경기대학교 이메일(@kgu.ac.kr 또는 @kyonggi.ac.kr)만 사용할 수 있습니다."
    )
    @Column(name = "email", nullable = false, unique = true, updatable = false)
    private String email;

    @NotBlank(message = "비밀번호를 입력해야 합니다.")
    @Size(min = 8, max = 12, message = "비밀번호는 8~12자여야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,12}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "소속 학과를 입력해야 합니다.")
    @Column(nullable = false)
    private String department;

    @NotBlank(message = "학번을 입력해야 합니다.")
    @Pattern(regexp = "^\\d{9}$", message = "학번은 숫자 9자리여야 합니다.")
    @Column(nullable = false, length = 9)
    private String studentId;

    @NotBlank(message = "이름을 입력해야 합니다.")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "전화번호를 입력해야 합니다.")
    @Pattern(
        regexp = "^010-\\d{4}-\\d{4}$",
        message = "전화번호는 010-1234-5678 형식으로 입력해야 합니다."
    )
    @Column(nullable = false, length = 13)
    private String phoneNumber;

    @NotBlank(message = "백준 아이디를 입력해야 합니다.")
    @Column(nullable = false, unique = true)
    private String bojId;

    @NotNull(message = "메인 언어를 선택해야 합니다.")
    @NotNull
    @Enumerated(EnumType.STRING)
    private MainLanguage mainLanguage;

    @Size(max = 256, message = "지원 동기는 최대 256자까지 입력할 수 있습니다.")
    @Column(length = 256)
    private String applyReason;

}
