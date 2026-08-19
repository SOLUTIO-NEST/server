package com.solutio.api.domain.recruitment.domain;

import com.solutio.api.domain.recruitment.dto.request.RecruitmentUpdateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RecruitmentTest {

    @Test
    @DisplayName("공고 생성 시 기본 상태는 UPCOMING이다")
    void create_defaultStatusIsUpcoming() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);

        Recruitment recruitment = Recruitment.create("테스트 공고", start, end);

        assertThat(recruitment.getStatus()).isEqualTo(RecruitmentStatus.UPCOMING);
    }

    @Test
    @DisplayName("update 호출 시 status를 포함한 필드들이 수정된다")
    void update_withStatus_updatesCorrectly() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(5);
        Recruitment recruitment = Recruitment.create("테스트 공고", start, end);

        RecruitmentUpdateRequestDto updateDto = RecruitmentUpdateRequestDto.builder()
                .title("수정된 공고")
                .status(RecruitmentStatus.OPEN)
                .build();

        recruitment.update(updateDto);

        assertThat(recruitment.getTitle()).isEqualTo("수정된 공고");
        assertThat(recruitment.getStatus()).isEqualTo(RecruitmentStatus.OPEN);
    }

    @Test
    @DisplayName("announcementDateTime이 지정되지 않으면 null로 설정된다")
    void create_withoutAnnouncementDateTime_announcementDateTimeIsNull() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        Recruitment recruitment = Recruitment.create("테스트 공고", start, end);

        assertThat(recruitment.getAnnouncementDateTime()).isNull();
        assertThat(recruitment.getIsApplicantDataPurged()).isFalse();
    }

    @Test
    @DisplayName("announcementDateTime이 null인 경우 파기 대상이 아니다")
    void isEligibleForApplicantPurge_announcementDateTimeIsNull_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().minusWeeks(8);
        LocalDateTime end = LocalDateTime.now().minusWeeks(7);

        Recruitment recruitment = Recruitment.create("테스트 공고", start, end);

        assertThat(recruitment.isEligibleForApplicantPurge()).isFalse();
    }

    @Test
    @DisplayName("발표일로부터 6주가 지난 경우 파기 대상으로 판별된다")
    void isEligibleForApplicantPurge_afterSixWeeks_returnsTrue() {
        LocalDateTime start = LocalDateTime.now().minusWeeks(8);
        LocalDateTime end = LocalDateTime.now().minusWeeks(7);
        LocalDateTime announcement = LocalDateTime.now().minusWeeks(6).minusDays(1);

        Recruitment recruitment = Recruitment.create("테스트 공고", start, end, announcement);

        assertThat(recruitment.isEligibleForApplicantPurge()).isTrue();
    }

    @Test
    @DisplayName("발표일로부터 6주가 지나지 않은 경우 파기 대상이 아니다")
    void isEligibleForApplicantPurge_beforeSixWeeks_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().minusWeeks(4);
        LocalDateTime end = LocalDateTime.now().minusWeeks(3);
        LocalDateTime announcement = LocalDateTime.now().minusWeeks(2);

        Recruitment recruitment = Recruitment.create("테스트 공고", start, end, announcement);

        assertThat(recruitment.isEligibleForApplicantPurge()).isFalse();
    }

    @Test
    @DisplayName("이미 파기 완료 처리된 경우 파기 대상에서 제외된다")
    void isEligibleForApplicantPurge_alreadyPurged_returnsFalse() {
        LocalDateTime start = LocalDateTime.now().minusWeeks(8);
        LocalDateTime end = LocalDateTime.now().minusWeeks(7);
        LocalDateTime announcement = LocalDateTime.now().minusWeeks(7);

        Recruitment recruitment = Recruitment.create("테스트 공고", start, end, announcement);
        recruitment.markApplicantDataPurged();

        assertThat(recruitment.isEligibleForApplicantPurge()).isFalse();
        assertThat(recruitment.getIsApplicantDataPurged()).isTrue();
    }
}
