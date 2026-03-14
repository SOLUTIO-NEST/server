package com.solutio.api.domain.applicant.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicantPassResponseDto {

    private String name;
    private String levelClass;
    private String groupAccountLink;
    private String groupAccountNumber;
    private Long recruitmentId;
    private String passedMessage;
    private Boolean isPassed;

    public static ApplicantPassResponseDto from(Applicant applicant, String groupAccountLink, String groupAccountNumber) {
        Recruitment recruitment = applicant.getRecruitment();
        return ApplicantPassResponseDto.builder()
            .name(applicant.getName())
            .levelClass((applicant.getLevelClass() != null) ? applicant.getLevelClass().getDescription() : "미정")
            .recruitmentId(recruitment.getId())
            .groupAccountLink(applicant.getIsApprove() ? groupAccountLink : null)
            .groupAccountNumber(applicant.getIsApprove() ? groupAccountNumber : null)
            .passedMessage(applicant.getIsApprove() ? recruitment.getPassedMessage() : null)
            .isPassed(applicant.getIsApprove())
            .build();
    }
}
