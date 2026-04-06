package com.solutio.api.domain.user.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.domain.user.dto.request.UserUpdateRequestDto;
import com.solutio.api.domain.user.dto.response.UserMyInfoResponseDto;
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

@ActiveProfiles("test")
@DataJpaTest
@Import({UserService.class, MemberService.class, BCryptPasswordEncoder.class})
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private MemberRepository memberRepository;

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

    private void createApplicant() {
        applicantRepository.save(Applicant.builder()
                .studentId(STUDENT_ID)
                .email(STUDENT_ID + "@kyonggi.ac.kr")
                .password("encoded_password")
                .department("컴퓨터공학부")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .bojId(STUDENT_ID + "_boj")
                .mainLanguage(MainLanguage.JAVA)
                .applyReason("지원 동기")
                .isApprove(false)
                .build());

        em.flush();
        em.clear();
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
    @DisplayName("ROLE_GUEST 사용자는 Applicant 정보를 반환한다")
    void getMyInfo_asGuest_returnsApplicantInfo() {
        createApplicant();
        givenAuthentication("202312345", "ROLE_GUEST");

        UserMyInfoResponseDto result = userService.getMyInfo();

        assertThat(result.studentId()).isEqualTo("202312345");
        assertThat(result.role()).isEqualTo(Role.GUEST.getDescription());
        assertThat(result.email()).isEqualTo("202312345@kyonggi.ac.kr");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.mainLanguage()).isEqualTo(MainLanguage.JAVA);
    }

    @Test
    @DisplayName("ROLE_USER 사용자는 Member 정보를 반환한다")
    void getMyInfo_asUser_returnsMemberInfo() {
        createMember();
        givenAuthentication("202312345", "ROLE_USER");

        UserMyInfoResponseDto result = userService.getMyInfo();

        assertThat(result.studentId()).isEqualTo("202312345");
        assertThat(result.role()).isEqualTo(Role.USER.getDescription());
        assertThat(result.classLevel()).isEqualTo(ClassLevel.SEED.getDescription());
        assertThat(result.email()).isEqualTo("202312345@kyonggi.ac.kr");
    }

    @Test
    @DisplayName("ROLE_GUEST인데 Applicant가 없으면 예외가 발생한다")
    void getMyInfo_asGuest_throwsWhenApplicantNotFound() {
        givenAuthentication("999999999", "ROLE_GUEST");

        assertThatThrownBy(() -> userService.getMyInfo()).isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("ROLE_USER인데 Member가 없으면 예외가 발생한다")
    void getMyInfo_asUser_throwsWhenMemberNotFound() {
        givenAuthentication("999999999", "ROLE_USER");

        assertThatThrownBy(() -> userService.getMyInfo()).isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("ROLE_GUEST 사용자는 Applicant 정보를 수정할 수 있다")
    void updateMyInfo_asGuest_updatesApplicantInfo() {
        createApplicant();
        givenAuthentication("202312345", "ROLE_GUEST");

        userService.updateMyInfo(new UserUpdateRequestDto("김철수", "소프트웨어공학과", "010-9999-8888", "new_boj", MainLanguage.PYTHON));

        em.flush();
        em.clear();

        Applicant updated = applicantRepository.findById("202312345").orElseThrow();
        assertThat(updated.getName()).isEqualTo("김철수");
        assertThat(updated.getDepartment()).isEqualTo("소프트웨어공학과");
        assertThat(updated.getPhoneNumber()).isEqualTo("010-9999-8888");
        assertThat(updated.getBojId()).isEqualTo("new_boj");
        assertThat(updated.getMainLanguage()).isEqualTo(MainLanguage.PYTHON);
    }

    @Test
    @DisplayName("ROLE_USER 사용자는 Member 정보를 수정할 수 있다")
    void updateMyInfo_asUser_updatesMemberInfo() {
        createMember();
        givenAuthentication("202312345", "ROLE_USER");

        userService.updateMyInfo(new UserUpdateRequestDto("김철수", "소프트웨어공학과", "010-9999-8888", "new_boj", MainLanguage.PYTHON));

        em.flush();
        em.clear();

        Member updated = memberRepository.findById("202312345").orElseThrow();
        assertThat(updated.getName()).isEqualTo("김철수");
        assertThat(updated.getDepartment()).isEqualTo("소프트웨어공학과");
        assertThat(updated.getPhoneNumber()).isEqualTo("010-9999-8888");
        assertThat(updated.getBojId()).isEqualTo("new_boj");
        assertThat(updated.getMainLanguage()).isEqualTo(MainLanguage.PYTHON);
    }

    @Test
    @DisplayName("ROLE_GUEST인데 Applicant가 없으면 예외가 발생한다")
    void updateMyInfo_asGuest_throwsWhenApplicantNotFound() {
        givenAuthentication("999999999", "ROLE_GUEST");

        assertThatThrownBy(() -> userService.updateMyInfo(new UserUpdateRequestDto("이름", null, null, null, null)))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("ROLE_USER인데 Member가 없으면 예외가 발생한다")
    void updateMyInfo_asUser_throwsWhenMemberNotFound() {
        givenAuthentication("999999999", "ROLE_USER");

        assertThatThrownBy(() -> userService.updateMyInfo(new UserUpdateRequestDto("이름", null, null, null, null)))
                .isInstanceOf(GeneralException.class);
    }
}
