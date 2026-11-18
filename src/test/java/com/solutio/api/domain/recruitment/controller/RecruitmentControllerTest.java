package com.solutio.api.domain.recruitment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.domain.RecruitmentStatus;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentCreateRequestDto;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentUpdateRequestDto;
import com.solutio.api.domain.recruitment.dto.response.RecruitmentResponseDto;
import com.solutio.api.domain.recruitment.service.RecruitmentService;
import com.solutio.api.global.auth.jwt.TokenProvider;
import com.solutio.api.global.auth.service.TokenRevocationService;
import com.solutio.api.global.config.SecurityConfig;
import com.solutio.api.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@WebMvcTest(RecruitmentController.class)
@Import(SecurityConfig.class)
class RecruitmentControllerTest {

    @Autowired
    MockMvcTester mvcTester;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RecruitmentService recruitmentService;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    TokenRevocationService tokenRevocationService;

    @Test
    @DisplayName("비로그인 사용자도 전체 모집 공고 목록을 조회할 수 있다")
    void retrieveRecruitments_anonymous_returns200() {
        LocalDateTime now = LocalDateTime.now();
        Recruitment recruitment1 = Recruitment.create("2기", now.plusDays(1), now.plusDays(10));
        ReflectionTestUtils.setField(recruitment1, "id", 1L);

        Recruitment recruitment2 = Recruitment.create("1기", now.minusMonths(2), now.minusMonths(1));
        ReflectionTestUtils.setField(recruitment2, "id", 2L);

        PageImpl<RecruitmentResponseDto> page = new PageImpl<>(List.of(
                RecruitmentResponseDto.from(recruitment1),
                RecruitmentResponseDto.from(recruitment2)
        ));
        given(recruitmentService.getRecruitments(any(Pageable.class))).willReturn(PageResponse.from(page));

        assertThat(mvcTester.get().uri("/api/v1/recruitments")
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.contents")
                .asArray()
                .hasSize(2);
    }

    @Test
    @DisplayName("비로그인 사용자도 특정 모집 공고의 단건 상세를 조회할 수 있다")
    void retrieveRecruitment_anonymous_returns200() {
        LocalDateTime now = LocalDateTime.now();
        Recruitment recruitment = Recruitment.create("1기 모집", now, now.plusDays(7));
        ReflectionTestUtils.setField(recruitment, "id", 1L);

        given(recruitmentService.getRecruitment(1L)).willReturn(recruitment);

        assertThat(mvcTester.get().uri("/api/v1/recruitments/1")
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.title")
                .isEqualTo("1기 모집");
    }

    @Test
    @DisplayName("ROLE_STAFF 사용자는 모집 공고를 생성할 수 있다")
    @WithMockUser(roles = "STAFF")
    void createRecruitment_asStaff_returns200() throws Exception {
        RecruitmentCreateRequestDto requestDto = new RecruitmentCreateRequestDto();
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(requestDto, "title", "새 공고");
        ReflectionTestUtils.setField(requestDto, "startDateTime", now);
        ReflectionTestUtils.setField(requestDto, "endDateTime", now.plusDays(10));

        given(recruitmentService.createRecruitment(any(RecruitmentCreateRequestDto.class))).willReturn(1L);

        assertThat(mvcTester.post().uri("/api/v1/recruitments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("ROLE_USER 사용자는 모집 공고를 생성할 수 없다 (401 Unauthorized)")
    @WithMockUser(roles = "USER")
    void createRecruitment_asUser_returns401() throws Exception {
        RecruitmentCreateRequestDto requestDto = new RecruitmentCreateRequestDto();
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(requestDto, "title", "새 공고");
        ReflectionTestUtils.setField(requestDto, "startDateTime", now);
        ReflectionTestUtils.setField(requestDto, "endDateTime", now.plusDays(10));

        assertThat(mvcTester.post().uri("/api/v1/recruitments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("ROLE_STAFF 사용자는 모집 공고 및 상태를 수정할 수 있다")
    @WithMockUser(roles = "STAFF")
    void updateRecruitment_asStaff_returns200() throws Exception {
        RecruitmentUpdateRequestDto updateDto = RecruitmentUpdateRequestDto.builder()
                .title("수정된 공고")
                .status(RecruitmentStatus.OPEN)
                .build();

        given(recruitmentService.updateRecruitment(eq(1L), any(RecruitmentUpdateRequestDto.class))).willReturn(1L);

        assertThat(mvcTester.patch().uri("/api/v1/recruitments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("ROLE_STAFF 사용자는 모집 공고를 삭제할 수 있다")
    @WithMockUser(roles = "STAFF")
    void deleteRecruitment_asStaff_returns200() {
        given(recruitmentService.deleteRecruitment(1L)).willReturn(1L);

        assertThat(mvcTester.delete().uri("/api/v1/recruitments/1")
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .isEqualTo(1);
    }
}
