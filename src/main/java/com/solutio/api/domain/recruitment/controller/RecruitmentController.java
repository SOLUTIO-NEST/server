package com.solutio.api.domain.recruitment.controller;

import com.solutio.api.domain.recruitment.dto.request.RecruitmentCreateRequestDto;
import com.solutio.api.domain.recruitment.dto.request.RecruitmentUpdateRequestDto;
import com.solutio.api.domain.recruitment.dto.response.RecruitmentResponseDto;
import com.solutio.api.domain.recruitment.service.RecruitmentService;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruitments")
@RequiredArgsConstructor
@Tag(name = "Recruitment", description = "모집 일정")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @Operation(summary = "[STAFF] 모집 공고 등록", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("")
    public ApiResponse<?> createRecruitment(
        @Valid @RequestBody RecruitmentCreateRequestDto requestDto
    ) {
        Long id = recruitmentService.createRecruitment(requestDto);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), id);
    }

    @Operation(summary = "모집 공고 조회", description = "ROLE_ANONYMOUS 이상의 권한이 필요함")
    @GetMapping("")
    public ApiResponse<RecruitmentResponseDto> retrieveRecruitment(
    ) {
        RecruitmentResponseDto response = RecruitmentResponseDto.from(recruitmentService.getRecruitment());
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), response);
    }

    @Operation(summary = "[STAFF] 모집 공고 수정", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @PatchMapping("/{recruitmentId}")
    public ApiResponse<?> updateRecruitment(
        @PathVariable(name = "recruitmentId") Long recruitmentId,
        @Valid @RequestBody RecruitmentUpdateRequestDto requestDto
    ) {
        Long id = recruitmentService.updateRecruitment(recruitmentId,requestDto);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), id);
    }

    @Operation(summary = "[STAFF] 모집 공고 삭제", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @DeleteMapping("/{recruitmentId}")
    public ApiResponse<?> deleteRecruitment(
        @PathVariable(name = "recruitmentId") Long recruitmentId
    ) {
        Long id = recruitmentService.deleteRecruitment(recruitmentId);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), id);
    }
}
