package com.solutio.api.domain.member.service;

import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.dto.request.MemberUpdateRequestDto;
import com.solutio.api.domain.member.dto.response.MemberMyInfoResponseDto;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.global.response.GeneralException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.login.repository.RefreshTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@DataJpaTest
@Import({MemberService.class, BCryptPasswordEncoder.class})
@Transactional
class MemberServiceTest {

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private EntityManager em;

    static final String STUDENT_ID = "202312345";

    private void givenAuthentication(String studentId, String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                new User(studentId, "", List.of(new SimpleGrantedAuthority(role))),
                null,
                List.of(new SimpleGrantedAuthority(role))
        ));
        SecurityContextHolder.setContext(context);
    }

    private void createMember() {
        memberRepository.save(Member.createFromApplicant(
                STUDENT_ID,
                STUDENT_ID + "@kyonggi.ac.kr",
                "encoded_password",
                "컴퓨터공학부",
                "홍길동",
                "010-1234-5678",
                STUDENT_ID + "_boj",
                MainLanguage.JAVA,
                ClassLevel.SEED
        ));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("ROLE_USER 사용자는 Member 정보를 반환한다")
    void getMyInfo_asUser_returnsMemberInfo() {
        createMember();
        givenAuthentication(STUDENT_ID, "ROLE_USER");

        MemberMyInfoResponseDto result = memberService.getMyInfo();

        assertThat(result.studentId()).isEqualTo(STUDENT_ID);
        assertThat(result.role()).isEqualTo(Role.USER.getDescription());
        assertThat(result.classLevel()).isEqualTo(ClassLevel.SEED.getDescription());
        assertThat(result.email()).isEqualTo(STUDENT_ID + "@kyonggi.ac.kr");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.mainLanguage()).isEqualTo(MainLanguage.JAVA);
    }

    @Test
    @DisplayName("Member가 없으면 예외가 발생한다")
    void getMyInfo_throwsWhenMemberNotFound() {
        givenAuthentication("999999999", "ROLE_USER");

        assertThatThrownBy(() -> memberService.getMyInfo()).isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("ROLE_USER 사용자는 Member 정보를 수정할 수 있다")
    void updateMyInfo_asUser_updatesMemberInfo() {
        createMember();
        givenAuthentication(STUDENT_ID, "ROLE_USER");

        memberService.updateMyInfo(new MemberUpdateRequestDto("김철수", "소프트웨어공학과", "010-9999-8888", "new_boj", MainLanguage.PYTHON));

        em.flush();
        em.clear();

        Member updated = memberRepository.findById(STUDENT_ID).orElseThrow();
        assertThat(updated.getName()).isEqualTo("김철수");
        assertThat(updated.getDepartment()).isEqualTo("소프트웨어공학과");
        assertThat(updated.getPhoneNumber()).isEqualTo("010-9999-8888");
        assertThat(updated.getBojId()).isEqualTo("new_boj");
        assertThat(updated.getMainLanguage()).isEqualTo(MainLanguage.PYTHON);
    }

    @Test
    @DisplayName("Member가 없으면 수정 시 예외가 발생한다")
    void updateMyInfo_throwsWhenMemberNotFound() {
        givenAuthentication("999999999", "ROLE_USER");

        assertThatThrownBy(() -> memberService.updateMyInfo(new MemberUpdateRequestDto("이름", null, null, null, null)))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("회원 탈퇴 시 Member가 soft delete되고 existsWithdrawnByStudentId가 true를 반환한다")
    void withdraw_softDeletesMember_andDeletesApplicant() {
        createMember();
        givenAuthentication(STUDENT_ID, "ROLE_USER");

        memberService.withdraw();

        em.flush();
        em.clear();

        assertThat(memberRepository.findById(STUDENT_ID)).isEmpty();
        assertThat(memberRepository.existsWithdrawnByStudentId(STUDENT_ID)).isTrue();
    }

    @Test
    @DisplayName("탈퇴한 회원이 재지원하여 합격 후 createMember 호출 시 기존 레코드가 재활성화된다")
    void createMember_reactivatesWithdrawnMember() {
        createMember();
        givenAuthentication(STUDENT_ID, "ROLE_USER");
        memberService.withdraw();

        em.flush();
        em.clear();

        Applicant reApplicant = Applicant.create(
                STUDENT_ID,
                null,
                "new_email@kyonggi.ac.kr",
                "new_password",
                "소프트웨어경영학과",
                "홍길동_재가입",
                "010-9999-0000",
                "new_boj",
                MainLanguage.PYTHON,
                "재지원",
                new BCryptPasswordEncoder()
        );

        Member reactivated = memberService.createMember(reApplicant);

        em.flush();
        em.clear();

        assertThat(reactivated.getStudentId()).isEqualTo(STUDENT_ID);
        Member found = memberRepository.findById(STUDENT_ID).orElseThrow();
        assertThat(found.getName()).isEqualTo("홍길동_재가입");
        assertThat(found.getDepartment()).isEqualTo("소프트웨어경영학과");
        assertThat(found.getMainLanguage()).isEqualTo(MainLanguage.PYTHON);
        assertThat(found.getRole()).isEqualTo(Role.USER);
    }
}
