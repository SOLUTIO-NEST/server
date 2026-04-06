package com.solutio.api.domain.user.integration;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.global.auth.jwt.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {
    @Autowired
    MockMvcTester mvcTester;

    @Autowired
    TokenProvider tokenProvider;

    @Autowired
    ApplicantRepository applicantRepository;

    @Autowired
    MemberRepository memberRepository;

    static final String STUDENT_ID = "202312345";

    private String generateToken(String studentId, String role) {
        return tokenProvider.generateToken(studentId, Duration.ofHours(1), role);
    }

    private MvcTestResultAssert request(String token) {
        return assertThat(mvcTester.get().uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON));
    }

    private Applicant createApplicant(String studentId) {
        return applicantRepository.save(Applicant.builder()
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
    }

    private Member createMember(String studentId) {
        return memberRepository.save(Member.createFromApplicant(
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
    }

    @Test
    @DisplayName("ROLE_GUEST 토큰으로 자신의 Applicant 정보를 조회할 수 있다")
    void getMyInfo_asGuest_returnsApplicantInfo() {
        createApplicant(STUDENT_ID);
        String token = generateToken(STUDENT_ID, "GUEST");

        var result = request(token);

        result.hasStatus2xxSuccessful()
                .bodyJson().extractingPath("$.data.studentId").isEqualTo(STUDENT_ID);
        result.bodyJson().extractingPath("$.data.role").isEqualTo(Role.GUEST.getDescription());
        result.bodyJson().extractingPath("$.data.email").isEqualTo(STUDENT_ID + "@kyonggi.ac.kr");
        result.bodyJson().extractingPath("$.data.name").isEqualTo("홍길동");
        result.bodyJson().extractingPath("$.data.department").isEqualTo("컴퓨터공학부");
        result.bodyJson().extractingPath("$.data.mainLanguage").isEqualTo("JAVA");
        result.bodyJson().extractingPath("$.data.classLevel").isNull();
    }

    @Test
    @DisplayName("ROLE_USER 토큰으로 자신의 Member 정보를 조회할 수 있다")
    void getMyInfo_asUser_returnsMemberInfo() {
        createMember(STUDENT_ID);
        String token = generateToken(STUDENT_ID, "USER");

        var result = request(token);

        result.hasStatus2xxSuccessful()
                .bodyJson().extractingPath("$.data.studentId").isEqualTo(STUDENT_ID);
        result.bodyJson().extractingPath("$.data.role").isEqualTo(Role.USER.getDescription());
        result.bodyJson().extractingPath("$.data.classLevel").isEqualTo(ClassLevel.SEED.getDescription());
        result.bodyJson().extractingPath("$.data.email").isEqualTo(STUDENT_ID + "@kyonggi.ac.kr");
    }

    @Test
    @DisplayName("토큰 없이 요청하면 403을 반환한다")
    void getMyInfo_withoutToken_returns403() {
        assertThat(mvcTester.get().uri("/api/v1/users/me")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("ROLE_GUEST 토큰이지만 Applicant가 없으면 404를 반환한다")
    void getMyInfo_asGuest_withoutApplicant_returns404() {
        String token = generateToken("999999999", "GUEST");

        request(token).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ROLE_USER 토큰이지만 Member가 없으면 404를 반환한다")
    void getMyInfo_asUser_withoutMember_returns404() {
        String token = generateToken("999999999", "USER");

        request(token).hasStatus(HttpStatus.NOT_FOUND);
    }
}
