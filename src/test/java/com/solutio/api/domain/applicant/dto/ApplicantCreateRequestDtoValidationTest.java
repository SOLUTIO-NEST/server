package com.solutio.api.domain.applicant.dto;

import com.solutio.api.domain.applicant.dto.request.ApplicantCreateRequestDto;
import com.solutio.api.domain.member.domain.MainLanguage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicantCreateRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private ApplicantCreateRequestDto createValidDto() {
        ApplicantCreateRequestDto dto = new ApplicantCreateRequestDto();
        ReflectionTestUtils.setField(dto, "studentId", "202600001");
        ReflectionTestUtils.setField(dto, "recruitmentId", 1L);
        ReflectionTestUtils.setField(dto, "email", "test@kyonggi.ac.kr");
        ReflectionTestUtils.setField(dto, "password", "password123!");
        ReflectionTestUtils.setField(dto, "department", "컴퓨터공학부");
        ReflectionTestUtils.setField(dto, "name", "홍길동");
        ReflectionTestUtils.setField(dto, "phoneNumber", "010-1234-5678");
        ReflectionTestUtils.setField(dto, "bojId", "boj_hong");
        ReflectionTestUtils.setField(dto, "mainLanguage", MainLanguage.JAVA);
        ReflectionTestUtils.setField(dto, "applyReason", "지원동기");
        return dto;
    }

    private String passwordOf(int length) {
        return "a".repeat(length);
    }

    @Test
    @DisplayName("모든 필드가 유효하면 제약 위반이 없다")
    void validDto_hasNoViolations() {
        Set<ConstraintViolation<ApplicantCreateRequestDto>> violations = validator.validate(createValidDto());
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 7자면 위반이 발생한다")
    void password_shorterThan8_hasViolation() {
        ApplicantCreateRequestDto dto = createValidDto();
        ReflectionTestUtils.setField(dto, "password", passwordOf(7));

        Set<ConstraintViolation<ApplicantCreateRequestDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 8~128자여야 합니다.");
    }

    @Test
    @DisplayName("비밀번호가 8자면 통과한다")
    void password_exactly8_passes() {
        ApplicantCreateRequestDto dto = createValidDto();
        ReflectionTestUtils.setField(dto, "password", passwordOf(8));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 128자면 통과한다")
    void password_exactly128_passes() {
        ApplicantCreateRequestDto dto = createValidDto();
        ReflectionTestUtils.setField(dto, "password", passwordOf(128));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 129자면 위반이 발생한다")
    void password_longerThan128_hasViolation() {
        ApplicantCreateRequestDto dto = createValidDto();
        ReflectionTestUtils.setField(dto, "password", passwordOf(129));

        Set<ConstraintViolation<ApplicantCreateRequestDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 8~128자여야 합니다.");
    }

    @Test
    @DisplayName("단순 숫자만으로 이루어진 8자 비밀번호도 조합 규칙 없이 통과한다")
    void password_simpleDigits_passes() {
        ApplicantCreateRequestDto dto = createValidDto();
        ReflectionTestUtils.setField(dto, "password", "12345678");

        assertThat(validator.validate(dto)).isEmpty();
    }
}
