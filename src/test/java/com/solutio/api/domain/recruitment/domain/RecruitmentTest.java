package com.solutio.api.domain.recruitment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RecruitmentTest {

    @Test
    @DisplayName("announcementDateTime이 지정되지 않으면 endDateTime으로 기본 설정된다")
    void create_withoutAnnouncementDateTime_defaultsToEndDateTime() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        Recruitment recruitment = Recruitment.create("테스트 공고", start, end);

        assertThat(recruitment.getAnnouncementDateTime()).isEqualTo(end);
        assertThat(recruitment.getIsApplicantDataPurged()).isFalse();
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
