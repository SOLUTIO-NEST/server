package com.solutio.api.domain.member.integration;

import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.member.repository.MemberRepository;
import com.solutio.api.global.auth.jwt.TokenProvider;
import jakarta.persistence.EntityManager;
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
class MemberControllerIntegrationTest {
    @Autowired
    MockMvcTester mvcTester;

    @Autowired
    TokenProvider tokenProvider;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager em;

    static final String STUDENT_ID = "202312345";

    private String generateToken(String studentId, String role) {
        return tokenProvider.generateToken(studentId, Duration.ofHours(1), role);
    }

    private MvcTestResultAssert request(String token) {
        return assertThat(mvcTester.get().uri("/api/v1/members/me")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON));
    }

    private MvcTestResultAssert updateRequest(String token, String body) {
        return assertThat(mvcTester.patch().uri("/api/v1/members/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
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
    }

    @Test
    @DisplayName("ROLE_USER 토큰으로 자신의 Member 정보를 조회할 수 있다")
    void getMyInfo_asUser_returnsMemberInfo() {
        createMember();
        String token = generateToken(STUDENT_ID, "USER");

        var result = request(token);

        result.hasStatus2xxSuccessful()
                .bodyJson().extractingPath("$.data.studentId").isEqualTo(STUDENT_ID);
        result.bodyJson().extractingPath("$.data.role").isEqualTo(Role.USER.getDescription());
        result.bodyJson().extractingPath("$.data.classLevel").isEqualTo(ClassLevel.SEED.getDescription());
        result.bodyJson().extractingPath("$.data.email").isEqualTo(STUDENT_ID + "@kyonggi.ac.kr");
        result.bodyJson().extractingPath("$.data.name").isEqualTo("홍길동");
        result.bodyJson().extractingPath("$.data.mainLanguage").isEqualTo("JAVA");
    }

    @Test
    @DisplayName("ROLE_GUEST 토큰으로는 정보를 조회할 수 없다")
    void getMyInfo_asGuest_returns403() {
        String token = generateToken(STUDENT_ID, "GUEST");

        request(token).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("토큰 없이 요청하면 403을 반환한다")
    void getMyInfo_withoutToken_returns403() {
        assertThat(mvcTester.get().uri("/api/v1/members/me")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Member가 없으면 404를 반환한다")
    void getMyInfo_withoutMember_returns404() {
        String token = generateToken("999999999", "USER");

        request(token).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ROLE_USER 토큰으로 Member 정보를 수정할 수 있다")
    void updateMyInfo_asUser_updatesMemberInfo() {
        createMember();
        String token = generateToken(STUDENT_ID, "USER");

        updateRequest(token, """
                {"name": "김철수", "department": "소프트웨어공학과", "phoneNumber": "010-9999-8888", "bojId": "new_boj", "mainLanguage": "PYTHON"}
                """).hasStatus2xxSuccessful();

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
    @DisplayName("ROLE_GUEST 토큰으로는 정보를 수정할 수 없다")
    void updateMyInfo_asGuest_returns403() {
        String token = generateToken(STUDENT_ID, "GUEST");

        updateRequest(token, """
                {"name": "김철수", "department": "소프트웨어공학과", "phoneNumber": "010-9999-8888", "bojId": "new_boj", "mainLanguage": "PYTHON"}
                """).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("토큰 없이 수정 요청하면 403을 반환한다")
    void updateMyInfo_withoutToken_returns403() {
        assertThat(mvcTester.patch().uri("/api/v1/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"김철수\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("전화번호 형식이 잘못되면 400을 반환한다")
    void updateMyInfo_invalidPhoneNumber_returns400() {
        createMember();
        String token = generateToken(STUDENT_ID, "USER");

        updateRequest(token, "{\"phoneNumber\": \"01012345678\"}").hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 400을 반환한다")
    void updateMyInfo_blankName_returns400() {
        createMember();
        String token = generateToken(STUDENT_ID, "USER");

        updateRequest(token, "{\"name\": \"   \"}").hasStatus(HttpStatus.BAD_REQUEST);
    }
}
