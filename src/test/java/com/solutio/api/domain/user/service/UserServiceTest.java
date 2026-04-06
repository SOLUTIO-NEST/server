package com.solutio.api.domain.user.service;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.domain.member.service.MemberService;
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

    private void givenAuthentication(String studentId, String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                new User(studentId, "", List.of(new SimpleGrantedAuthority(role))),
                null,
                List.of(new SimpleGrantedAuthority(role))
        ));
        SecurityContextHolder.setContext(context);
    }

    private Applicant createApplicant(String studentId) {
        Applicant applicant = applicantRepository.save(Applicant.builder()
                .studentId(studentId)
                .email(studentId + "@kyonggi.ac.kr")
                .password("encoded_password")
                .department("컴퓨터공학부")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .bojId(studentId + "_boj")
                .mainLanguage(MainLanguage.JAVA)
                .applyReason("지원 동기")
                .isApprove(false)
                .build());

        em.flush();
        em.clear();

        return applicant;
    }

    private Member createMember(String studentId) {
        Member member = memberRepository.save(Member.createFromApplicant(
                studentId,
                studentId + "@kyonggi.ac.kr",
                "encoded_password",
                "컴퓨터공학부",
                "홍길동",
                "010-1234-5678",
                studentId + "_boj",
                MainLanguage.JAVA,
                ClassLevel.SEED
        ));

        em.flush();
        em.clear();

        return member;
    }

    @Test
    @DisplayName("ROLE_GUEST 사용자는 Applicant 정보를 반환한다")
    void getMyInfo_asGuest_returnsApplicantInfo() {
        createApplicant("202312345");
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
        createMember("202312345");
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
}
