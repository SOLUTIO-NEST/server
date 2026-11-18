package com.solutio.api.domain.applicant.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
import com.solutio.api.domain.applicant.dto.request.ApplicantCreateRequestDto;
import com.solutio.api.domain.applicant.dto.response.ApplicantDetailResponseDto;
import com.solutio.api.domain.applicant.dto.response.ApplicantResponseDto;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.domain.RecruitmentStatus;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
import com.solutio.api.domain.recruitment.service.RecruitmentService;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.PageResponse;
import com.solutio.api.global.response.Status;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solutio.api.domain.login.repository.RefreshTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@DataJpaTest
@Import({ApplicantService.class, MemberService.class, RecruitmentService.class, BCryptPasswordEncoder.class})
@Transactional
class ApplicantServiceTest {

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ApplicantService applicantService;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private Recruitment createRecruitment() {
        Recruitment recruitment = Recruitment.create(
                "2026 Recruitment",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        ReflectionTestUtils.setField(recruitment, "status", RecruitmentStatus.OPEN);
        return recruitmentRepository.save(recruitment);
    }

    @Test
    @DisplayName("신청 시 기본 passStatus는 PENDING, classLevel은 UNASSIGNED이다")
    void applyMember_createsApplicantWithPendingAndUnassigned() {
        Recruitment recruitment = createRecruitment();

        ApplicantCreateRequestDto requestDto = new ApplicantCreateRequestDto();
        ReflectionTestUtils.setField(requestDto, "studentId", "202600001");
        ReflectionTestUtils.setField(requestDto, "recruitmentId", recruitment.getId());
        ReflectionTestUtils.setField(requestDto, "email", "test@kyonggi.ac.kr");
        ReflectionTestUtils.setField(requestDto, "password", "password123!");
        ReflectionTestUtils.setField(requestDto, "department", "컴퓨터공학부");
        ReflectionTestUtils.setField(requestDto, "name", "홍길동");
        ReflectionTestUtils.setField(requestDto, "phoneNumber", "010-1234-5678");
        ReflectionTestUtils.setField(requestDto, "bojId", "boj_hong");
        ReflectionTestUtils.setField(requestDto, "mainLanguage", MainLanguage.JAVA);
        ReflectionTestUtils.setField(requestDto, "applyReason", "지원동기");

        String studentId = applicantService.applyMember(requestDto);

        em.flush();
        em.clear();

        Applicant applicant = applicantRepository.findById(studentId).orElseThrow();
        assertThat(applicant.getPassStatus()).isEqualTo(PassStatus.PENDING);
        assertThat(applicant.getClassLevel()).isEqualTo(ClassLevel.UNASSIGNED);
    }

    @Test
    @DisplayName("approveApplicant는 지원자의 passStatus를 APPROVED로 변경한다")
    void approveApplicant_changesStatusToApproved() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = applicantRepository.save(Applicant.create(
                "202600001", recruitment, "test@kyonggi.ac.kr", "pass", "컴공", "홍길동",
                "010-1234-5678", "boj_hong", MainLanguage.JAVA, "동기", new BCryptPasswordEncoder()
        ));

        applicantService.approveApplicant(applicant.getStudentId());

        em.flush();
        em.clear();

        Applicant updated = applicantRepository.findById(applicant.getStudentId()).orElseThrow();
        assertThat(updated.getPassStatus()).isEqualTo(PassStatus.APPROVED);
    }

    @Test
    @DisplayName("rejectApplicant는 지원자의 passStatus를 REJECTED로 변경한다")
    void rejectApplicant_changesStatusToRejected() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = applicantRepository.save(Applicant.create(
                "202600001", recruitment, "test@kyonggi.ac.kr", "pass", "컴공", "홍길동",
                "010-1234-5678", "boj_hong", MainLanguage.JAVA, "동기", new BCryptPasswordEncoder()
        ));

        applicantService.rejectApplicant(applicant.getStudentId());

        em.flush();
        em.clear();

        Applicant updated = applicantRepository.findById(applicant.getStudentId()).orElseThrow();
        assertThat(updated.getPassStatus()).isEqualTo(PassStatus.REJECTED);
    }

    @Test
    @DisplayName("passStatus가 PENDING인 경우 Member 생성 시 예외가 발생한다")
    void createMemberByRecruitment_pendingStatus_throwsException() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = applicantRepository.save(Applicant.create(
                "202600001", recruitment, "test@kyonggi.ac.kr", "pass", "컴공", "홍길동",
                "010-1234-5678", "boj_hong", MainLanguage.JAVA, "동기", new BCryptPasswordEncoder()
        ));

        assertThatThrownBy(() -> applicantService.createMemberByRecruitment(applicant.getStudentId()))
                .isInstanceOf(GeneralException.class)
                .extracting("status")
                .isEqualTo(Status.NOT_APPROVED_APPLICANT);
    }

    @Test
    @DisplayName("passStatus가 REJECTED인 경우 Member 생성 시 예외가 발생한다")
    void createMemberByRecruitment_rejectedStatus_throwsException() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = applicantRepository.save(Applicant.create(
                "202600001", recruitment, "test@kyonggi.ac.kr", "pass", "컴공", "홍길동",
                "010-1234-5678", "boj_hong", MainLanguage.JAVA, "동기", new BCryptPasswordEncoder()
        ));
        applicant.reject();
        applicantRepository.save(applicant);

        assertThatThrownBy(() -> applicantService.createMemberByRecruitment(applicant.getStudentId()))
                .isInstanceOf(GeneralException.class)
                .extracting("status")
                .isEqualTo(Status.NOT_APPROVED_APPLICANT);
    }

    @Test
    @DisplayName("passStatus가 APPROVED인 지원자만 Member 생성이 가능하다")
    void createMemberByRecruitment_approvedStatus_createsMember() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = applicantRepository.save(Applicant.create(
                "202600001", recruitment, "test@kyonggi.ac.kr", "pass", "컴공", "홍길동",
                "010-1234-5678", "boj_hong", MainLanguage.JAVA, "동기", new BCryptPasswordEncoder()
        ));
        applicant.approve();
        applicantRepository.save(applicant);

        String createdId = applicantService.createMemberByRecruitment(applicant.getStudentId());

        assertThat(createdId).isEqualTo("202600001");
        Member member = memberRepository.findById("202600001").orElseThrow();
        assertThat(member.getClassLevel()).isEqualTo(ClassLevel.UNASSIGNED);
    }

    @Test
    @DisplayName("createMembersByRecruitment는 APPROVED 상태의 지원자 목록만 Member로 전환한다")
    void createMembersByRecruitment_convertsOnlyApprovedApplicants() {
        Recruitment recruitment = createRecruitment();

        Applicant app1 = Applicant.create(
                "202600001", recruitment, "app1@kyonggi.ac.kr", "pass", "컴공", "김합격",
                "010-1111-2222", "boj1", MainLanguage.JAVA, "동기1", new BCryptPasswordEncoder()
        );
        Applicant app2 = Applicant.create(
                "202600002", recruitment, "app2@kyonggi.ac.kr", "pass", "컴공", "이대기",
                "010-3333-4444", "boj2", MainLanguage.PYTHON, "동기2", new BCryptPasswordEncoder()
        );
        Applicant app3 = Applicant.create(
                "202600003", recruitment, "app3@kyonggi.ac.kr", "pass", "컴공", "박불합",
                "010-5555-6666", "boj3", MainLanguage.C, "동기3", new BCryptPasswordEncoder()
        );

        app1.approve();
        app3.reject();

        applicantRepository.save(app1);
        applicantRepository.save(app2);
        applicantRepository.save(app3);

        List<String> createdIds = applicantService.createMembersByRecruitment(recruitment.getId());

        assertThat(createdIds).containsExactly("202600001");
    }

    @Test
    @DisplayName("ApplicantDetailResponseDto 및 ApplicantResponseDto는 passStatus와 description을 올바르게 반환한다")
    void getApplicant_and_getApplicants_returnPassStatusAndClassLevelDescription() {
        Recruitment recruitment = createRecruitment();
        Applicant applicant = applicantRepository.save(Applicant.create(
                "202600001", recruitment, "test@kyonggi.ac.kr", "pass", "컴공", "홍길동",
                "010-1234-5678", "boj_hong", MainLanguage.JAVA, "동기", new BCryptPasswordEncoder()
        ));

        ApplicantDetailResponseDto detailDto = applicantService.getApplicant(applicant.getStudentId());
        assertThat(detailDto.getPassStatus()).isEqualTo(PassStatus.PENDING);
        assertThat(detailDto.getClassLevel()).isEqualTo("미배정");

        PageResponse<ApplicantResponseDto> pageResponse = applicantService.getApplicants(recruitment.getId(), PageRequest.of(0, 10));
        assertThat(pageResponse.getContents()).hasSize(1);
        assertThat(pageResponse.getContents().get(0).getPassStatus()).isEqualTo(PassStatus.PENDING);
        assertThat(pageResponse.getContents().get(0).getClassLevel()).isEqualTo("미배정");
    }
}
