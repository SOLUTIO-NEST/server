package com.solutio.api.domain.applicant.scheduler;

import com.solutio.api.domain.applicant.service.ApplicantPurgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicantPurgeScheduler {

    private final ApplicantPurgeService applicantPurgeService;

    /**
     * 매일 새벽 3시에 실행되어 최종 발표일 기준 6주가 지난 모집 공고의 지원자 데이터를 자동 파기합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runApplicantDataPurgeJob() {
        log.info("[스케줄러 시작] 6주 경과 지원자 개인정보 자동 파기 작업 시작");
        try {
            int deletedCount = applicantPurgeService.purgeExpiredApplicantData();
            log.info("[스케줄러 완료] 지원자 개인정보 자동 파기 작업 완료 - 총 파기된 지원자 수: {}명", deletedCount);
        } catch (Exception e) {
            log.error("[스케줄러 오류] 지원자 개인정보 자동 파기 작업 중 예외 발생", e);
        }
    }
}
