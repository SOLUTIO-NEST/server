package com.solutio.api.domain.applicant.scheduler;

import com.solutio.api.domain.applicant.service.ApplicantPurgeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicantPurgeSchedulerTest {

    @Mock
    private ApplicantPurgeService applicantPurgeService;

    @InjectMocks
    private ApplicantPurgeScheduler applicantPurgeScheduler;

    @Test
    @DisplayName("스케줄러 작업 실행 시 purgeExpiredApplicantData를 호출한다")
    void runApplicantDataPurgeJob_callsPurgeExpiredApplicantData() {
        given(applicantPurgeService.purgeExpiredApplicantData()).willReturn(5);

        applicantPurgeScheduler.runApplicantDataPurgeJob();

        verify(applicantPurgeService).purgeExpiredApplicantData();
    }
}
