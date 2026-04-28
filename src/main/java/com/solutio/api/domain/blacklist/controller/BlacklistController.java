package com.solutio.api.domain.blacklist.controller;

import com.solutio.api.domain.blacklist.dto.request.BlacklistAddRequestDto;
import com.solutio.api.domain.blacklist.dto.request.BlacklistUpdateReasonRequestDto;
import com.solutio.api.domain.blacklist.dto.response.BlacklistDetailResponseDto;
import com.solutio.api.domain.blacklist.dto.response.BlacklistResponseDto;
import com.solutio.api.domain.blacklist.service.BlacklistService;
import com.solutio.api.global.request.BasePageRequest;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blacklists")
@RequiredArgsConstructor
@Tag(name = "Blacklist", description = "블랙리스트")
public class BlacklistController {

    private final BlacklistService blacklistService;

    @Operation(summary = "[Staff] 블랙리스트 등록", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("")
    public ApiResponse<Long> addBlacklist(
        @Valid @RequestBody BlacklistAddRequestDto requestDto
    ) {
        Long id = blacklistService.addBlacklist(requestDto.getStudentId(), requestDto.getReason());
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), id);
    }

    @Operation(summary = "[Staff] 블랙리스트 사유 수정", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @PatchMapping("/{id}")
    public ApiResponse<Long> updateReason(
        @PathVariable(name = "id") Long id,
        @Valid @RequestBody BlacklistUpdateReasonRequestDto requestDto
    ) {
        Long resultId = blacklistService.updateReason(id, requestDto.getReason());
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), resultId);
    }

    @Operation(summary = "[Staff] 블랙리스트 삭제", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @DeleteMapping("/{id}")
    public ApiResponse<Long> deleteBlacklist(
        @PathVariable(name = "id") Long id
    ) {
        Long resultId = blacklistService.deleteBlacklist(id);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), resultId);
    }

    @Operation(summary = "[Staff] 블랙리스트 상세 조회", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/{id}")
    public ApiResponse<BlacklistDetailResponseDto> getBlacklist(
        @PathVariable(name = "id") Long id
    ) {
        BlacklistDetailResponseDto response = blacklistService.getBlacklist(id);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), response);
    }

    @Operation(summary = "[Staff] 블랙리스트 목록 조회", description = "ROLE_STAFF 이상의 권한이 필요함")
    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("")
    public ApiResponse<PageResponse<BlacklistResponseDto>> getBlacklists(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        BasePageRequest pageRequest = new BasePageRequest(page, size);
        PageResponse<BlacklistResponseDto> response = blacklistService.getBlacklists(pageRequest.toPageable());
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), response);
    }
}
