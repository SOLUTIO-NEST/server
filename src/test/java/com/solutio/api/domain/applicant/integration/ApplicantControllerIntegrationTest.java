package com.solutio.api.domain.applicant.integration;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.domain.RecruitmentStatus;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
import com.solutio.api.global.auth.jwt.TokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicantControllerIntegrationTest {

    @Autowired
    MockMvcTester mvcTester;

    @Autowired
    TokenProvider tokenProvider;

    @Autowired
    ApplicantRepository applicantRepository;

    @Autowired
    RecruitmentRepository recruitmentRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager em;

    static final String STUDENT_ID = "202612345";

    private String generateToken(String studentId, String role) {
        return tokenProvider.generateToken(studentId, Duration.ofHours(1), role);
    }

    private Recruitment createRecruitment() {
        Recruitment recruitment = Recruitment.create(
                "2026 신입 모집",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        ReflectionTestUtils.setField(recruitment, "status", RecruitmentStatus.OPEN);
        return recruitmentRepository.save(recruitment);
    }

    private Applicant createApplicant(Recruitment recruitment) {
        return applicantRepository.save(Applicant.create(
                STUDENT_ID,
                recruitment,
                STUDENT_ID + "@kyonggi.ac.kr",
                "password123!",
                "컴퓨터공학부",
                "홍길동",
                "010-1234-5678",
                STUDENT_ID + "_boj",
                MainLanguage.JAVA,
                "지원동기입니다",
                new BCryptPasswordEncoder()
        ));
    }

    @Test
    @DisplayName("신규 지원 시 passStatus는 PENDING, classLevel은 UNASSIGNED로 저장된다")
    void applyForClub_savesDefaultPassStatusAndClassLevel() {
        Recruitment recruitment = createRecruitment();

        var result = assertThat(mvcTester.post().uri("/api/v1/applicants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "studentId": "202699999",
                            "recruitmentId": %d,
                            "email": "newapp@kyonggi.ac.kr",
                            "password": "password123!",
                            "department": "컴퓨터공학부",
                            "name": "신규지원",
                            "phoneNumber": "010-9999-8888",
                            "bojId": "new_boj",
                            "mainLanguage": "JAVA",
                            "applyReason": "신청합니다"
                        }
                        """.formatted(recruitment.getId())));

        result.hasStatus2xxSuccessful();

        Applicant applicant = applicantRepository.findById("202699999").orElseThrow();
        assertThat(applicant.getPassStatus()).isEqualTo(PassStatus.PENDING);
        assertThat(applicant.getClassLevel()).isEqualTo(ClassLevel.UNASSIGNED);
    }

    @Test
    @DisplayName("STAFF 권한으로 지원자 합격 처리 시 passStatus가 APPROVED로 업데이트된다")
    void approveApplicant_asStaff_updatesPassStatusToApproved() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = createApplicant(recruitment);

        String staffToken = generateToken("202000001", "STAFF");

        var result = assertThat(mvcTester.patch().uri("/api/v1/applicants/approve/" + applicant.getStudentId())
                .header("Authorization", "Bearer " + staffToken));

        result.hasStatus2xxSuccessful();

        em.flush();
        em.clear();

        Applicant updated = applicantRepository.findById(applicant.getStudentId()).orElseThrow();
        assertThat(updated.getPassStatus()).isEqualTo(PassStatus.APPROVED);
    }

    @Test
    @DisplayName("STAFF 권한으로 지원자 불합격 처리 시 passStatus가 REJECTED로 업데이트된다")
    void rejectApplicant_asStaff_updatesPassStatusToRejected() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = createApplicant(recruitment);

        String staffToken = generateToken("202000001", "STAFF");

        var result = assertThat(mvcTester.patch().uri("/api/v1/applicants/reject/" + applicant.getStudentId())
                .header("Authorization", "Bearer " + staffToken));

        result.hasStatus2xxSuccessful();

        em.flush();
        em.clear();

        Applicant updated = applicantRepository.findById(applicant.getStudentId()).orElseThrow();
        assertThat(updated.getPassStatus()).isEqualTo(PassStatus.REJECTED);
    }

    @Test
    @DisplayName("PENDING 상태의 신청자는 개별 회원 전환 시 409 CONFLICT를 반환한다")
    void registerMember_pendingStatus_returns409() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = createApplicant(recruitment);

        String nestToken = generateToken("202000001", "NEST");

        var result = assertThat(mvcTester.post().uri("/api/v1/applicants/" + applicant.getStudentId())
                .header("Authorization", "Bearer " + nestToken));

        result.hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("APPROVED 상태의 신청자만 개별 회원 전환이 성공한다")
    void registerMember_approvedStatus_succeeds() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = createApplicant(recruitment);
        applicant.approve();
        applicantRepository.save(applicant);

        String nestToken = generateToken("202000001", "NEST");

        var result = assertThat(mvcTester.post().uri("/api/v1/applicants/" + applicant.getStudentId())
                .header("Authorization", "Bearer " + nestToken));

        result.hasStatus2xxSuccessful();
        assertThat(memberRepository.findById(applicant.getStudentId())).isPresent();
    }

    @Test
    @DisplayName("합격 여부 조회 시 passStatus를 포함한 DTO를 반환한다")
    void checkApplicantPass_returnsPassStatus() {
        Recruitment recruitment = Recruitment.create(
                "2026 종료된 모집",
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now().minusDays(1)
        );
        ReflectionTestUtils.setField(recruitment, "status", RecruitmentStatus.CLOSED);
        recruitmentRepository.save(recruitment);

        Applicant applicant = createApplicant(recruitment);
        applicant.approve();
        applicantRepository.save(applicant);

        String guestToken = generateToken(applicant.getStudentId(), "GUEST");

        var result = assertThat(mvcTester.get().uri("/api/v1/applicants/my")
                .header("Authorization", "Bearer " + guestToken)
                .accept(MediaType.APPLICATION_JSON));

        result.hasStatus2xxSuccessful()
                .bodyJson().extractingPath("$.data.passStatus").isEqualTo("APPROVED");
        result.bodyJson().extractingPath("$.data.classLevel").isEqualTo("미배정");
    }

    @Test
    @DisplayName("STAFF 권한으로 지원자 데이터 수동 파기를 요청하면 성공한다")
    void purgeApplicantData_asStaff_purgesApplicantsSuccessfully() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = createApplicant(recruitment);
        applicantRepository.save(applicant);

        String staffToken = generateToken("STAFF001", "STAFF");

        var result = assertThat(mvcTester.post().uri("/api/v1/applicants/purge/" + recruitment.getId())
                .header("Authorization", "Bearer " + staffToken));

        result.hasStatus2xxSuccessful();
        assertThat(applicantRepository.findAllByRecruitmentId(recruitment.getId(), org.springframework.data.domain.Pageable.unpaged())).isEmpty();
    }
}
