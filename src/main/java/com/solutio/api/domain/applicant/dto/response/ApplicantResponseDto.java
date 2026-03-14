package com.solutio.api.domain.applicant.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApplicantResponseDto {
    private final String studentId;
    private final String name;
    private final String department;
    private final String phoneNumber;
    private final Boolean isApprove;
    private final String classLevel;
    private final LocalDateTime createdAt;

    public static ApplicantResponseDto from(Applicant applicant) {
        return ApplicantResponseDto.builder()
                .studentId(applicant.getStudentId())
                .name(applicant.getName())
                .department(applicant.getDepartment())
                .phoneNumber(applicant.getPhoneNumber())
                .isApprove(applicant.getIsApprove())
                .classLevel(applicant.getClassLevel() == null ? null: applicant.getClassLevel().getDescription())
                .createdAt(applicant.getCreatedAt())
                .build();
    }
}
