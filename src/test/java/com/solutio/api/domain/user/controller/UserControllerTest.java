package com.solutio.api.domain.user.controller;

import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Role;
import com.solutio.api.domain.user.dto.response.UserMyInfoResponseDto;
import com.solutio.api.domain.user.service.UserService;
import com.solutio.api.global.auth.jwt.TokenProvider;
import com.solutio.api.global.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

@ActiveProfiles("test")
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {
    @Autowired
    MockMvcTester mvcTester;

    @MockitoBean
    UserService userService;

    @MockitoBean
    TokenProvider tokenProvider;

    private UserMyInfoResponseDto guestInfo() {
        return new UserMyInfoResponseDto(
                "202312345",
                "202312345@kyonggi.ac.kr",
                Role.GUEST.getDescription(),
                null,
                "홍길동",
                "컴퓨터공학부",
                "010-1234-5678",
                "202312345_boj",
                MainLanguage.JAVA
        );
    }

    private UserMyInfoResponseDto memberInfo() {
        return new UserMyInfoResponseDto(
                "202312345",
                "202312345@kyonggi.ac.kr",
                Role.USER.getDescription(),
                "SEED",
                "홍길동",
                "컴퓨터공학부",
                "010-1234-5678",
                "202312345_boj",
                MainLanguage.JAVA
        );
    }

    @Test
    @DisplayName("ROLE_GUEST 사용자는 자신의 정보를 조회할 수 있다")
    @WithMockUser(roles = "GUEST")
    void getMyInfo_asGuest_returns200() {
        given(userService.getMyInfo()).willReturn(guestInfo());

        assertThat(mvcTester.get().uri("/api/v1/users/me")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.data.studentId").isEqualTo("202312345");
    }

    @Test
    @DisplayName("ROLE_USER 사용자는 자신의 정보를 조회할 수 있다")
    @WithMockUser(roles = "USER")
    void getMyInfo_asUser_returns200() {
        given(userService.getMyInfo()).willReturn(memberInfo());

        assertThat(mvcTester.get().uri("/api/v1/users/me")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatus2xxSuccessful()
                .bodyJson()
                .extractingPath("$.data.role").isEqualTo(Role.USER.getDescription());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 자신의 정보를 조회할 수 없다")
    void getMyInfo_unauthenticated_returns403() {
        assertThat(mvcTester.get().uri("/api/v1/users/me")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("ROLE_GUEST 사용자는 자신의 정보를 수정할 수 있다")
    @WithMockUser(roles = "GUEST")
    void updateMyInfo_asGuest_returns200() {
        willDoNothing().given(userService).updateMyInfo(any());

        assertThat(mvcTester.patch().uri("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"김철수\", \"department\": \"컴퓨터공학부\", \"phoneNumber\": \"010-1234-5678\", \"bojId\": \"new_boj\", \"mainLanguage\": \"JAVA\"}"))
                .hasStatus2xxSuccessful();
    }

    @Test
    @DisplayName("ROLE_USER 사용자는 자신의 정보를 수정할 수 있다")
    @WithMockUser(roles = "USER")
    void updateMyInfo_asUser_returns200() {
        willDoNothing().given(userService).updateMyInfo(any());

        assertThat(mvcTester.patch().uri("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"홍길동\", \"department\": \"컴퓨터공학부\", \"phoneNumber\": \"010-1234-5678\", \"bojId\": \"new_boj\", \"mainLanguage\": \"PYTHON\"}"))
                .hasStatus2xxSuccessful();
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 자신의 정보를 수정할 수 없다")
    void updateMyInfo_unauthenticated_returns403() {
        assertThat(mvcTester.patch().uri("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"김철수\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("전화번호 형식이 잘못되면 400을 반환한다")
    @WithMockUser(roles = "USER")
    void updateMyInfo_invalidPhoneNumber_returns400() {
        assertThat(mvcTester.patch().uri("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\": \"01012345678\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 400을 반환한다")
    @WithMockUser(roles = "USER")
    void updateMyInfo_blankName_returns400() {
        assertThat(mvcTester.patch().uri("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"   \"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}