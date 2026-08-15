package com.solutio.api.domain.applicant.service;

import com.solutio.api.domain.applicant.repository.ApplicantRepository;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import com.solutio.api.domain.recruitment.repository.RecruitmentRepository;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicantPurgeService {

    private final RecruitmentRepository recruitmentRepository;
    private final ApplicantRepository applicantRepository;

    /**
     * 최종 발표일 기준 6주가 경과한 모든 모집 공고의 지원자 데이터를 자동 파기합니다.
     *
     * @return 파기 처리된 총 지원자 수
     */
    @Transactional
    public int purgeExpiredApplicantData() {
        LocalDateTime baseTime = LocalDateTime.now().minusWeeks(6);
        List<Recruitment> targetRecruitments = recruitmentRepository.findAllEligibleForApplicantPurge(baseTime);

        int totalDeletedCount = 0;
        for (Recruitment recruitment : targetRecruitments) {
            if (recruitment.isEligibleForApplicantPurge()) {
                recruitment.markApplicantDataPurged();
                int deletedCount = applicantRepository.deleteAllByRecruitmentId(recruitment.getId());
                totalDeletedCount += deletedCount;

                log.info("[개인정보 파기 완료] 모집 공고 ID: {}, 제목: '{}', 파기된 지원자 수: {}명",
                        recruitment.getId(), recruitment.getTitle(), deletedCount);
            }
        }
        return totalDeletedCount;
    }

    /**
     * 특정 모집 공고의 지원자 데이터를 수동으로 즉시 파기합니다.
     *
     * @param recruitmentId 모집 공고 ID
     * @return 파기 처리된 지원자 수
     */
    @Transactional
    public int purgeApplicantDataByRecruitmentId(Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new GeneralException(Status.RECRUITMENT_NOT_FOUND));

        recruitment.markApplicantDataPurged();
        int deletedCount = applicantRepository.deleteAllByRecruitmentId(recruitment.getId());

        log.info("[개인정보 수동 파기 완료] 모집 공고 ID: {}, 제목: '{}', 파기된 지원자 수: {}명",
                recruitment.getId(), recruitment.getTitle(), deletedCount);

        return deletedCount;
    }
}
