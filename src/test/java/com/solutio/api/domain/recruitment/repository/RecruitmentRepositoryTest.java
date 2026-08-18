package com.solutio.api.domain.recruitment.repository;

import com.solutio.api.domain.recruitment.domain.Recruitment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class RecruitmentRepositoryTest {

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Test
    @DisplayName("findAllEligibleForApplicantPurge는 6주 경과 및 미파기 상태인 모집 공고만 조회한다")
    void findAllEligibleForApplicantPurge_returnsOnlyEligibleRecruitments() {
        LocalDateTime baseTime = LocalDateTime.now().minusWeeks(6);

        // 1. 6주 경과 & 미파기 대상 공고 (조회되어야 함)
        Recruitment eligible = recruitmentRepository.save(Recruitment.create(
                "6주 경과 공고",
                LocalDateTime.now().minusWeeks(8),
                LocalDateTime.now().minusWeeks(7),
                LocalDateTime.now().minusWeeks(6).minusDays(1)
        ));

        // 2. 6주 미경과 공고 (조회 안 됨)
        Recruitment notExpired = recruitmentRepository.save(Recruitment.create(
                "최근 공고",
                LocalDateTime.now().minusWeeks(3),
                LocalDateTime.now().minusWeeks(2),
                LocalDateTime.now().minusWeeks(1)
        ));

        // 3. 6주 경과했지만 이미 파기 처리된 공고 (조회 안 됨)
        Recruitment alreadyPurged = Recruitment.create(
                "이미 파기된 공고",
                LocalDateTime.now().minusWeeks(8),
                LocalDateTime.now().minusWeeks(7),
                LocalDateTime.now().minusWeeks(7)
        );
        alreadyPurged.markApplicantDataPurged();
        recruitmentRepository.save(alreadyPurged);

        // 4. 종료일은 6주가 지났으나 발표일이 미정(null)인 공고 (조회 안 됨)
        Recruitment noAnnouncementDate = recruitmentRepository.save(Recruitment.create(
                "발표일 미정 공고",
                LocalDateTime.now().minusWeeks(8),
                LocalDateTime.now().minusWeeks(7)
        ));

        List<Recruitment> result = recruitmentRepository.findAllEligibleForApplicantPurge(baseTime);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(eligible.getId());
    }

    @Test
    @DisplayName("findAllByOrderByStartDateTimeDesc는 시작일시 내림차순(최신순)으로 정렬하여 조회한다")
    void findAllByOrderByStartDateTimeDesc_returnsRecruitmentsOrderedByStartDateTimeDesc() {
        LocalDateTime now = LocalDateTime.now();

        Recruitment firstRecruitment = recruitmentRepository.save(Recruitment.create(
                "1기 모집",
                now.minusMonths(6),
                now.minusMonths(5)
        ));

        Recruitment thirdRecruitment = recruitmentRepository.save(Recruitment.create(
                "3기 모집",
                now.plusMonths(1),
                now.plusMonths(2)
        ));

        Recruitment secondRecruitment = recruitmentRepository.save(Recruitment.create(
                "2기 모집",
                now.minusMonths(1),
                now.plusDays(10)
        ));

        List<Recruitment> result = recruitmentRepository.findAllByOrderByStartDateTimeDesc();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Recruitment::getTitle)
                .containsExactly("3기 모집", "2기 모집", "1기 모집");
    }
}
