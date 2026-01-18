package com.solutio.api.domain.applicant.controller;

import com.solutio.api.domain.applicant.service.ApplicantService;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applicants")
@RequiredArgsConstructor
@Tag(name = "Applicant", description = "지원자")
public class ApplicantController {
    private final ApplicantService applicantService;

    @Operation(summary = "[N] 합격자 계정 통합 생성", description = "ROLE_NEST 이상의 권한이 필요함")
    @PreAuthorize("hasRole('NEST')")
    @PostMapping("/{recruitmentId}")
    public ApiResponse<?> registerMembersByRecruitment(
        @PathVariable(name = "recruitmentId") Long recruitmentId
    ) {
        List<String> ids = applicantService.createMembersByRecruitment(recruitmentId);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), ids);
    }

    @Operation(summary = "[N] 합격자 계정 개별 생성", description = "ROLE_NEST 이상의 권한이 필요함")
    @PreAuthorize("hasRole('NEST')")
    @PostMapping("/{recruitmentId}/{studentId}")
    public ApiResponse<?> registerMembersByRecruitment(
        @PathVariable(name = "recruitmentId") Long recruitmentId,
        @PathVariable(name = "studentId") String studentId
    ) {
        String id = applicantService.createMemberByRecruitment(recruitmentId, studentId);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), id);
    }
}
