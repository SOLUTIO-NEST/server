package com.solutio.api.domain.recruitment;

import com.solutio.api.domain.recruitment.domain.Recruitment;
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
}
