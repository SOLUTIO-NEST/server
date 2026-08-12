package com.solutio.api.domain.applicant.domain;

import com.solutio.api.domain.applicant.dto.request.ApplicantUpdateClassLevelRequestDto;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicantTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private Applicant createDefaultApplicant() {
        Recruitment recruitment = Recruitment.create(
                "2026 1학기 신입부원 모집",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );

        return Applicant.create(
                "202600001",
                recruitment,
                "test@kyonggi.ac.kr",
                "password123!",
                "컴퓨터공학부",
                "홍길동",
                "010-1234-5678",
                "boj_hong",
                MainLanguage.JAVA,
                "지원 동기입니다.",
                passwordEncoder
        );
    }

    @Test
    @DisplayName("신규 Applicant 생성 시 passStatus는 PENDING, classLevel은 UNASSIGNED이다")
    void create_defaultValues_arePendingAndUnassigned() {
        Applicant applicant = createDefaultApplicant();

        assertThat(applicant.getPassStatus()).isEqualTo(PassStatus.PENDING);
        assertThat(applicant.getClassLevel()).isEqualTo(ClassLevel.UNASSIGNED);
    }

    @Test
    @DisplayName("approve() 호출 시 passStatus가 APPROVED로 변경된다")
    void approve_changesPassStatusToApproved() {
        Applicant applicant = createDefaultApplicant();

        applicant.approve();

        assertThat(applicant.getPassStatus()).isEqualTo(PassStatus.APPROVED);
        assertThat(applicant.isApproved()).isTrue();
    }

    @Test
    @DisplayName("reject() 호출 시 passStatus가 REJECTED로 변경된다")
    void reject_changesPassStatusToRejected() {
        Applicant applicant = createDefaultApplicant();

        applicant.reject();

        assertThat(applicant.getPassStatus()).isEqualTo(PassStatus.REJECTED);
    }

    @Test
    @DisplayName("updateClassLevel() 호출 시 classLevel이 전달받은 값으로 변경된다")
    void updateClassLevel_updatesClassLevel() {
        Applicant applicant = createDefaultApplicant();
        ApplicantUpdateClassLevelRequestDto requestDto = new ApplicantUpdateClassLevelRequestDto();
        ReflectionTestUtils.setField(requestDto, "classLevel", ClassLevel.SEED);

        applicant.updateClassLevel(requestDto);

        assertThat(applicant.getClassLevel()).isEqualTo(ClassLevel.SEED);
    }

    @Test
    @DisplayName("updateClassLevel() 호출 시 null이 전달되면 UNASSIGNED로 설정된다")
    void updateClassLevel_null_setsUnassigned() {
        Applicant applicant = createDefaultApplicant();
        ApplicantUpdateClassLevelRequestDto requestDto = new ApplicantUpdateClassLevelRequestDto();
        ReflectionTestUtils.setField(requestDto, "classLevel", null);

        applicant.updateClassLevel(requestDto);

        assertThat(applicant.getClassLevel()).isEqualTo(ClassLevel.UNASSIGNED);
    }
}
