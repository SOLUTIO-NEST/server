package com.solutio.api.domain.login.dto;

import com.solutio.api.domain.login.dto.request.LoginRequestDto;
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

class LoginRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private LoginRequestDto createDto(String id, String password) {
        LoginRequestDto dto = new LoginRequestDto();
        ReflectionTestUtils.setField(dto, "id", id);
        ReflectionTestUtils.setField(dto, "password", password);
        return dto;
    }

    @Test
    @DisplayName("학번 9자리 숫자와 유효한 비밀번호면 제약 위반이 없다")
    void validDto_hasNoViolations() {
        assertThat(validator.validate(createDto("202312000", "password123!"))).isEmpty();
    }

    @Test
    @DisplayName("학번이 8자리 숫자면 위반이 발생한다")
    void studentId_8digits_hasViolation() {
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(createDto("20231200", "password123!"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("학번은 숫자 9자리여야 합니다.");
    }

    @Test
    @DisplayName("학번이 10자리 숫자면 위반이 발생한다")
    void studentId_10digits_hasViolation() {
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(createDto("2023120001", "password123!"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("학번은 숫자 9자리여야 합니다.");
    }

    @Test
    @DisplayName("학번이 숫자가 아니면 위반이 발생한다")
    void studentId_notDigits_hasViolation() {
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(createDto("2023a0001", "password123!"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("학번은 숫자 9자리여야 합니다.");
    }

    @Test
    @DisplayName("비밀번호가 128자면 통과한다")
    void password_exactly128_passes() {
        assertThat(validator.validate(createDto("202312000", "a".repeat(128)))).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 129자면 위반이 발생한다")
    void password_longerThan128_hasViolation() {
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(createDto("202312000", "a".repeat(129)));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 최대 128자까지 입력할 수 있습니다.");
    }
}
