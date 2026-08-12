package com.solutio.api.domain.applicant.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
import com.solutio.api.domain.member.domain.ClassLevel;
import com.solutio.api.domain.recruitment.domain.Recruitment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicantPassResponseDto {

    private String name;
    private String classLevel;
    private String groupAccountLink;
    private String groupAccountNumber;
    private Long recruitmentId;
    private String passedMessage;
    private PassStatus passStatus;

    public static ApplicantPassResponseDto from(Applicant applicant, String groupAccountLink, String groupAccountNumber) {
        Recruitment recruitment = applicant.getRecruitment();
        boolean isApproved = applicant.isApproved();
        return ApplicantPassResponseDto.builder()
            .name(applicant.getName())
            .classLevel((applicant.getClassLevel() != null) ? applicant.getClassLevel().getDescription() : ClassLevel.UNASSIGNED.getDescription())
            .recruitmentId(recruitment.getId())
            .groupAccountLink(isApproved ? groupAccountLink : null)
            .groupAccountNumber(isApproved ? groupAccountNumber : null)
            .passedMessage(isApproved ? recruitment.getPassedMessage() : null)
            .passStatus(applicant.getPassStatus())
            .build();
    }
}
