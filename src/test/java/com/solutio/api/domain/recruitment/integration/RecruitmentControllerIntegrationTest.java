package com.solutio.api.domain.recruitment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.domain.RecruitmentStatus;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentCreateRequestDto;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentUpdateRequestDto;
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
class RecruitmentControllerIntegrationTest {

    @Autowired
    MockMvcTester mvcTester;

    @Autowired
    TokenProvider tokenProvider;

    @Autowired
    RecruitmentRepository recruitmentRepository;

    @Autowired
    EntityManager em;

    @Autowired
    ObjectMapper objectMapper;

    private String generateToken(String studentId, String role) {
        return tokenProvider.generateToken(studentId, Duration.ofHours(1), role);
    }

    @Test
    @DisplayName("비로그인 사용자가 전체 모집 공고 목록을 시작일시 최신순으로 조회한다")
    void retrieveRecruitments_integration_returnsOrderedList() {
        LocalDateTime now = LocalDateTime.now();

        recruitmentRepository.save(Recruitment.create("1기 공고", now.minusMonths(3), now.minusMonths(2)));
        recruitmentRepository.save(Recruitment.create("3기 공고", now.plusMonths(1), now.plusMonths(2)));
        recruitmentRepository.save(Recruitment.create("2기 공고", now.minusMonths(1), now.plusDays(10)));

        assertThat(mvcTester.get().uri("/api/v1/recruitments")
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.contents")
                .asArray()
                .hasSize(3);

        assertThat(mvcTester.get().uri("/api/v1/recruitments")
                .contentType(MediaType.APPLICATION_JSON))
                .bodyJson()
                .extractingPath("$.data.contents[0].title")
                .isEqualTo("3기 공고");
    }

    @Test
    @DisplayName("비로그인 사용자가 특정 모집 공고를 단건 상세 조회한다")
    void retrieveRecruitment_integration_returnsDetail() {
        LocalDateTime now = LocalDateTime.now();
        Recruitment saved = recruitmentRepository.save(Recruitment.create("2기 모집", now, now.plusDays(14)));

        assertThat(mvcTester.get().uri("/api/v1/recruitments/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.title")
                .isEqualTo("2기 모집");
    }

    @Test
    @DisplayName("STAFF 권한으로 모집 공고를 등록한다")
    void createRecruitment_integration_asStaff_success() throws Exception {
        String staffToken = generateToken("20240001", "STAFF");

        RecruitmentCreateRequestDto requestDto = new RecruitmentCreateRequestDto();
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(requestDto, "title", "새로운 공고");
        ReflectionTestUtils.setField(requestDto, "startDateTime", now);
        ReflectionTestUtils.setField(requestDto, "endDateTime", now.plusDays(14));

        assertThat(mvcTester.post().uri("/api/v1/recruitments")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    @DisplayName("STAFF 권한으로 모집 공고 및 상태(status)를 수정한다")
    void updateRecruitment_integration_asStaff_updatesStatus() throws Exception {
        String staffToken = generateToken("20240001", "STAFF");
        LocalDateTime now = LocalDateTime.now();
        Recruitment saved = recruitmentRepository.save(Recruitment.create("기존 공고", now, now.plusDays(14)));

        RecruitmentUpdateRequestDto updateDto = RecruitmentUpdateRequestDto.builder()
                .title("수정된 공고 제목")
                .status(RecruitmentStatus.OPEN)
                .build();

        assertThat(mvcTester.patch().uri("/api/v1/recruitments/" + saved.getId())
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .hasStatus(HttpStatus.OK);

        em.flush();
        em.clear();

        Recruitment updated = recruitmentRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 공고 제목");
        assertThat(updated.getStatus()).isEqualTo(RecruitmentStatus.OPEN);
    }

    @Test
    @DisplayName("STAFF 권한으로 모집 공고를 삭제하면 조회 목록에서 제외된다")
    void deleteRecruitment_integration_asStaff_softDeletes() {
        String staffToken = generateToken("20240001", "STAFF");
        LocalDateTime now = LocalDateTime.now();
        Recruitment saved = recruitmentRepository.save(Recruitment.create("삭제될 공고", now, now.plusDays(14)));

        assertThat(mvcTester.delete().uri("/api/v1/recruitments/" + saved.getId())
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.OK);

        em.flush();
        em.clear();

        assertThat(recruitmentRepository.findById(saved.getId())).isEmpty();
    }
}
