package com.solutio.api.domain.recruitment.service;

import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.domain.RecruitmentStatus;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentCreateRequestDto;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentUpdateRequestDto;
import com.solutio.api.domain.recruitment.dto.response.RecruitmentResponseDto;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
@Import(RecruitmentService.class)
@Transactional
class RecruitmentServiceTest {

    @Autowired
    private RecruitmentService recruitmentService;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("getRecruitments는 시작일시 기준 최신순으로 정렬된 모집 공고 목록을 페이징하여 반환한다")
    void getRecruitments_returnsOrderedPage() {
        LocalDateTime now = LocalDateTime.now();

        recruitmentRepository.save(Recruitment.create("1기", now.minusMonths(3), now.minusMonths(2)));
        recruitmentRepository.save(Recruitment.create("3기", now.plusMonths(1), now.plusMonths(2)));
        recruitmentRepository.save(Recruitment.create("2기", now.minusMonths(1), now.plusDays(5)));

        PageResponse<RecruitmentResponseDto> results = recruitmentService.getRecruitments(PageRequest.of(0, 2));

        assertThat(results.getTotalElements()).isEqualTo(3);
        assertThat(results.getContents()).hasSize(2);
        assertThat(results.getContents()).extracting(RecruitmentResponseDto::getTitle)
                .containsExactly("3기", "2기");
    }

    @Test
    @DisplayName("getRecruitment(id)는 존재하는 공고의 id로 정상 조회한다")
    void getRecruitment_existingId_returnsRecruitment() {
        LocalDateTime now = LocalDateTime.now();
        Recruitment saved = recruitmentRepository.save(Recruitment.create("1기 모집", now, now.plusDays(7)));

        Recruitment found = recruitmentService.getRecruitment(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTitle()).isEqualTo("1기 모집");
    }

    @Test
    @DisplayName("getRecruitment(id)는 존재하지 않는 id일 때 RECRUITMENT_NOT_FOUND 예외를 던진다")
    void getRecruitment_nonExistingId_throwsException() {
        assertThatThrownBy(() -> recruitmentService.getRecruitment(9999L))
                .isInstanceOf(GeneralException.class)
                .hasFieldOrPropertyWithValue("status", Status.RECRUITMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("createRecruitment는 새로운 모집 공고를 생성한다")
    void createRecruitment_createsSuccessfully() {
        RecruitmentCreateRequestDto requestDto = new RecruitmentCreateRequestDto();
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(requestDto, "title", "새 공고");
        ReflectionTestUtils.setField(requestDto, "startDateTime", now);
        ReflectionTestUtils.setField(requestDto, "endDateTime", now.plusDays(10));

        Long id = recruitmentService.createRecruitment(requestDto);

        Recruitment found = recruitmentRepository.findById(id).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("새 공고");
        assertThat(found.getStatus()).isEqualTo(RecruitmentStatus.UPCOMING);
    }

    @Test
    @DisplayName("updateRecruitment는 공고 정보 및 status를 수정한다")
    void updateRecruitment_updatesStatusAndInfo() {
        LocalDateTime now = LocalDateTime.now();
        Recruitment saved = recruitmentRepository.save(Recruitment.create("기존 공고", now, now.plusDays(7)));

        RecruitmentUpdateRequestDto updateDto = RecruitmentUpdateRequestDto.builder()
                .title("수정 공고")
                .status(RecruitmentStatus.OPEN)
                .build();

        recruitmentService.updateRecruitment(saved.getId(), updateDto);
        em.flush();
        em.clear();

        Recruitment updated = recruitmentRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정 공고");
        assertThat(updated.getStatus()).isEqualTo(RecruitmentStatus.OPEN);
    }

    @Test
    @DisplayName("deleteRecruitment는 소프트 딜리트 처리된다")
    void deleteRecruitment_softDeletes() {
        LocalDateTime now = LocalDateTime.now();
        Recruitment saved = recruitmentRepository.save(Recruitment.create("삭제할 공고", now, now.plusDays(7)));

        recruitmentService.deleteRecruitment(saved.getId());
        em.flush();
        em.clear();

        assertThat(recruitmentRepository.findById(saved.getId())).isEmpty();
    }
}
