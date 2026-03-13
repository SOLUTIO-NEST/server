package com.solutio.api.domain.applicant.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicantPassResponseDto {

    private String name;
    private String groupAccountLink;
    private String groupAccountNumber;
    private Long recruitmentId;
    private Boolean isPassed;

    public static ApplicantPassResponseDto from(Applicant applicant,String groupAccountLink, String groupAccountNumber) {
        return ApplicantPassResponseDto.builder()
            .name(applicant.getName())
            .recruitmentId(applicant.getRecruitment().getId())
            .groupAccountLink(applicant.getIsApprove() ? groupAccountLink : null)
            .groupAccountLink(applicant.getIsApprove() ? groupAccountNumber : null)
            .isPassed(applicant.getIsApprove())
            .build();
    }
}
