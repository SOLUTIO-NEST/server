package com.solutio.api.domain.recruitment.service;

import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentCreateRequestDto;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentUpdateRequestDto;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitmentService {
    private final RecruitmentRepository recruitmentRepository;

    public Recruitment getRecruitment(Long recruitmentId) {
        return recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GeneralException(Status.RECRUITMENT_NOT_FOUND));
    }

    public Recruitment getRecruitment() {
        return recruitmentRepository.findFirstByIsDeleted(false);
    }

    @Transactional
    public Long createRecruitment(RecruitmentCreateRequestDto requestDto) {
        Recruitment recruitment = Recruitment.create(
            requestDto.getTitle(),
            requestDto.getStartDateTime(),
            requestDto.getEndDateTime()
        );

        return recruitmentRepository.save(recruitment).getId();
    }

    @Transactional
    public Long updateRecruitment(Long recruitmentId, RecruitmentUpdateRequestDto requestDto) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GeneralException(Status.RECRUITMENT_NOT_FOUND));

        recruitment.update(
            requestDto.getTitle(),
            requestDto.getStartDateTime(),
            requestDto.getEndDateTime()
        );

        return recruitment.getId();
    }

    @Transactional
    public Long deleteRecruitment(Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GeneralException(Status.RECRUITMENT_NOT_FOUND));

        recruitment.delete();

        return recruitment.getId();
    }
}
