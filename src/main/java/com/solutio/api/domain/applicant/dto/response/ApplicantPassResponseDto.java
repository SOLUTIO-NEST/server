package com.solutio.api.domain.applicant.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicantPassResponseDto {

    private String name;
    private Long recruitmentId;
    private Boolean isPassed;

    public static ApplicantPassResponseDto from(Applicant applicant) {
        return ApplicantPassResponseDto.builder()
            .name(applicant.getName())
            .recruitmentId(applicant.getRecruitment().getId())
            .isPassed(applicant.getIsApprove())
            .build();
    }
}
