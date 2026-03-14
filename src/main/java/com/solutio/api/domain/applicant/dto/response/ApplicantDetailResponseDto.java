package com.solutio.api.domain.applicant.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.member.domain.MainLanguage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApplicantDetailResponseDto {
    private final String studentId;
    private final String name;
    private final String department;
    private final String phoneNumber;
    private final String email;
    private final String bojId;
    private final MainLanguage mainLanguage;
    private final String applyReason;
    private final Boolean isApprove;
    private final String classLevel;
    private final LocalDateTime createdAt;

    public static ApplicantDetailResponseDto from(Applicant applicant) {
        return ApplicantDetailResponseDto.builder()
                .studentId(applicant.getStudentId())
                .name(applicant.getName())
                .department(applicant.getDepartment())
                .phoneNumber(applicant.getPhoneNumber())
                .email(applicant.getEmail())
                .bojId(applicant.getBojId())
                .mainLanguage(applicant.getMainLanguage())
                .applyReason(applicant.getApplyReason())
                .isApprove(applicant.getIsApprove())
                .classLevel(applicant.getClassLevel() == null ? null: applicant.getClassLevel().getDescription())
                .createdAt(applicant.getCreatedAt())
                .build();
    }
}
