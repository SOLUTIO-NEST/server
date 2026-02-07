package com.solutio.api.domain.blacklist.domain;

import com.solutio.api.global.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Blacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@(kgu\\.ac\\.kr|kyonggi\\.ac\\.kr)$",
        message = "경기대학교 이메일(@kgu.ac.kr 또는 @kyonggi.ac.kr)만 사용할 수 있습니다."
    )
    private String email;

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

    private String reason;

}
