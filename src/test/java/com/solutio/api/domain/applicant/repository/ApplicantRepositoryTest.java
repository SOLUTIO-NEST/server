package com.solutio.api.domain.applicant.repository;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class ApplicantRepositoryTest {

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("findByRecruitmentIdAndPassStatus는 해당 Recruitment와 PassStatus에 해당하는 지원자를 조회한다")
    void findByRecruitmentIdAndPassStatus_returnsMatchingApplicants() {
        Recruitment recruitment = recruitmentRepository.save(Recruitment.create(
                "2026 1학기 recruitment",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        ));

        Applicant applicant1 = Applicant.create(
                "202600001", recruitment, "app1@kyonggi.ac.kr", "pass", "학과", "김지원",
                "010-1111-2222", "boj1", MainLanguage.JAVA, "이유1", passwordEncoder
        );
        Applicant applicant2 = Applicant.create(
                "202600002", recruitment, "app2@kyonggi.ac.kr", "pass", "학과", "이지원",
                "010-3333-4444", "boj2", MainLanguage.PYTHON, "이유2", passwordEncoder
        );

        applicant1.approve();
        applicantRepository.save(applicant1);
        applicantRepository.save(applicant2);

        List<Applicant> approvedList = applicantRepository.findByRecruitmentIdAndPassStatus(recruitment.getId(), PassStatus.APPROVED);
        List<Applicant> pendingList = applicantRepository.findByRecruitmentIdAndPassStatus(recruitment.getId(), PassStatus.PENDING);
        List<Applicant> rejectedList = applicantRepository.findByRecruitmentIdAndPassStatus(recruitment.getId(), PassStatus.REJECTED);

        assertThat(approvedList).hasSize(1);
        assertThat(approvedList.get(0).getStudentId()).isEqualTo("202600001");
        assertThat(approvedList.get(0).getClassLevel()).isEqualTo(ClassLevel.UNASSIGNED);

        assertThat(pendingList).hasSize(1);
        assertThat(pendingList.get(0).getStudentId()).isEqualTo("202600002");

        assertThat(rejectedList).isEmpty();
    }

    @Test
    @DisplayName("deleteAllByRecruitmentId는 해당 recruitment의 모든 지원자를 벌크 삭제한다")
    void deleteAllByRecruitmentId_deletesAllApplicantsOfRecruitment() {
        Recruitment recruitment = recruitmentRepository.save(Recruitment.create(
                "삭제 대상 공고",
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5)
        ));

        Applicant applicant1 = Applicant.create(
                "202600010", recruitment, "app10@kyonggi.ac.kr", "pass", "학과", "박지원",
                "010-5555-6666", "boj10", MainLanguage.JAVA, "이유10", passwordEncoder
        );
        Applicant applicant2 = Applicant.create(
                "202600011", recruitment, "app11@kyonggi.ac.kr", "pass", "학과", "최지원",
                "010-7777-8888", "boj11", MainLanguage.C, "이유11", passwordEncoder
        );
        applicantRepository.save(applicant1);
        applicantRepository.save(applicant2);

        int deletedCount = applicantRepository.deleteAllByRecruitmentId(recruitment.getId());

        assertThat(deletedCount).isEqualTo(2);
        assertThat(applicantRepository.findAllByRecruitmentId(recruitment.getId(), org.springframework.data.domain.Pageable.unpaged())).isEmpty();
    }
}
