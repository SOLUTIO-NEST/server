package com.solutio.api.domain.applicant.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.applicant.domain.PassStatus;
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
    private final PassStatus passStatus;
    private final String classLevel;
    private final LocalDateTime createdAt;

    public static ApplicantResponseDto from(Applicant applicant) {
        return ApplicantResponseDto.builder()
                .studentId(applicant.getStudentId())
                .name(applicant.getName())
                .department(applicant.getDepartment())
                .phoneNumber(applicant.getPhoneNumber())
                .passStatus(applicant.getPassStatus())
                .classLevel(applicant.getClassLevel() == null ? null : applicant.getClassLevel().getDescription())
                .createdAt(applicant.getCreatedAt())
                .build();
    }
}
