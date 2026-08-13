package com.solutio.api.domain.applicant.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
import com.solutio.api.global.response.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
@Import({ApplicantPurgeService.class})
@Transactional
class ApplicantPurgeServiceTest {

    @Autowired
    private ApplicantPurgeService applicantPurgeService;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("purgeExpiredApplicantData는 6주가 지난 모집 공고의 지원자 데이터만 파기한다")
    void purgeExpiredApplicantData_purgesOnlyExpiredRecruitmentApplicants() {
        // 1. 6주 경과 공고 생성 및 지원자 2명 저장
        Recruitment expiredRecruitment = recruitmentRepository.save(Recruitment.create(
                "오래된 공고",
                LocalDateTime.now().minusWeeks(9),
                LocalDateTime.now().minusWeeks(8),
                LocalDateTime.now().minusWeeks(7)
        ));

        Applicant app1 = Applicant.create(
                "202600101", expiredRecruitment, "app101@kyonggi.ac.kr", "pass", "학과", "홍길동",
                "010-1111-2222", "boj101", MainLanguage.JAVA, "이유1", passwordEncoder
        );
        Applicant app2 = Applicant.create(
                "202600102", expiredRecruitment, "app102@kyonggi.ac.kr", "pass", "학과", "김철수",
                "010-3333-4444", "boj102", MainLanguage.PYTHON, "이유2", passwordEncoder
        );
        applicantRepository.save(app1);
        applicantRepository.save(app2);

        // 2. 최근 공고 생성 및 지원자 1명 저장
        Recruitment activeRecruitment = recruitmentRepository.save(Recruitment.create(
                "최근 공고",
                LocalDateTime.now().minusWeeks(2),
                LocalDateTime.now().minusWeeks(1),
                LocalDateTime.now().minusDays(3)
        ));

        Applicant app3 = Applicant.create(
                "202600103", activeRecruitment, "app103@kyonggi.ac.kr", "pass", "학과", "이영희",
                "010-5555-6666", "boj103", MainLanguage.C, "이유3", passwordEncoder
        );
        applicantRepository.save(app3);

        // 3. 자동 파기 로직 실행
        int deletedCount = applicantPurgeService.purgeExpiredApplicantData();

        // 4. 검증
        assertThat(deletedCount).isEqualTo(2);

        // 오래된 공고 지원자는 삭제됨
        assertThat(applicantRepository.findAllByRecruitmentId(expiredRecruitment.getId(), Pageable.unpaged())).isEmpty();
        
        // 오래된 공고는 파기 상태로 업데이트됨
        Recruitment updatedExpired = recruitmentRepository.findById(expiredRecruitment.getId()).orElseThrow();
        assertThat(updatedExpired.getIsApplicantDataPurged()).isTrue();

        // 최근 공고 지원자는 남아 있음
        assertThat(applicantRepository.findAllByRecruitmentId(activeRecruitment.getId(), Pageable.unpaged())).hasSize(1);
    }

    @Test
    @DisplayName("purgeApplicantDataByRecruitmentId는 해당 공고의 지원자 데이터를 수동 즉시 파기한다")
    void purgeApplicantDataByRecruitmentId_manuallyPurgesApplicants() {
        Recruitment recruitment = recruitmentRepository.save(Recruitment.create(
                "수동 파기 대상 공고",
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5)
        ));

        Applicant app = Applicant.create(
                "202600201", recruitment, "app201@kyonggi.ac.kr", "pass", "학과", "강감찬",
                "010-9999-8888", "boj201", MainLanguage.JAVA, "이유", passwordEncoder
        );
        applicantRepository.save(app);

        int deletedCount = applicantPurgeService.purgeApplicantDataByRecruitmentId(recruitment.getId());

        assertThat(deletedCount).isEqualTo(1);
        assertThat(applicantRepository.findAllByRecruitmentId(recruitment.getId(), Pageable.unpaged())).isEmpty();

        Recruitment updated = recruitmentRepository.findById(recruitment.getId()).orElseThrow();
        assertThat(updated.getIsApplicantDataPurged()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 공고 ID로 수동 파기 요청 시 예외가 발생한다")
    void purgeApplicantDataByRecruitmentId_invalidId_throwsException() {
        assertThatThrownBy(() -> applicantPurgeService.purgeApplicantDataByRecruitmentId(99999L))
                .isInstanceOf(GeneralException.class);
    }
}
