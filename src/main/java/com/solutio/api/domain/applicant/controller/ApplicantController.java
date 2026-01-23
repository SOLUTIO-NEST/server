package com.solutio.api.domain.applicant.controller;

import com.solutio.api.domain.applicant.dto.ApplicantCreateRequestDto;
import com.solutio.api.domain.applicant.service.ApplicantService;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applicants")
@RequiredArgsConstructor
@Tag(name = "Applicant", description = "지원자")
public class ApplicantController {
    private final ApplicantService applicantService;

    @Operation(summary = "동아리 지원", description = "ROLE_ANONYMOUS 이상의 권한이 필요함")
    @PostMapping("")
    public ApiResponse<String> applyForClub(
        @Valid @RequestBody ApplicantCreateRequestDto requestDto
    ) {
        String id = applicantService.applyMember(requestDto);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), id);
    }

    @Operation(summary = "[Nest] 합격자 계정 통합 생성", description = "ROLE_NEST 이상의 권한이 필요함")
    @PreAuthorize("hasRole('NEST')")
    @PostMapping("/batch/{recruitmentId}")
    public ApiResponse<?> registerMembersByRecruitment(
        @PathVariable(name = "recruitmentId") Long recruitmentId
    ) {
        List<String> ids = applicantService.createMembersByRecruitment(recruitmentId);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), ids);
    }

    @Operation(summary = "[Nest] 합격자 계정 개별 생성", description = "ROLE_NEST 이상의 권한이 필요함")
    @PreAuthorize("hasRole('NEST')")
    @PostMapping("/{studentId}")
    public ApiResponse<?> registerMembersByRecruitment(
        @PathVariable(name = "studentId") String studentId
    ) {
        String id = applicantService.createMemberByRecruitment(studentId);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), id);
    }

    @Operation(summary = "[Staff] 지원자 합격 처리", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @PatchMapping("/approve/{studentId}")
    public ApiResponse<?> approveApplicant(
        @PathVariable(name = "studentId") String studentId
    ) {
        String resultStudentId = applicantService.approveApplicant(studentId);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), resultStudentId);
    }

    @Operation(summary = "[Staff] 지원자 불합격 처리", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @PatchMapping("/reject/{studentId}")
    public ApiResponse<?> rejectApplicant(
        @PathVariable(name = "studentId") String studentId
    ) {
        String resultStudentId = applicantService.rejectApplicant(studentId);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), resultStudentId);
    }
}
