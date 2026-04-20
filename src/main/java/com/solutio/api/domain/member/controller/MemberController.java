package com.solutio.api.domain.member.controller;

import com.solutio.api.domain.member.dto.request.MemberUpdateRequestDto;
import com.solutio.api.domain.member.dto.response.MemberMyInfoResponseDto;
import com.solutio.api.domain.member.service.MemberService;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "멤버")
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "[User] 내 정보 조회", description = "ROLE_USER 이상의 권한이 필요함")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    public ApiResponse<MemberMyInfoResponseDto> getMyInfo() {
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), memberService.getMyInfo());
    }

    @Operation(summary = "[User] 내 정보 수정", description = "ROLE_USER 이상의 권한이 필요함. 수정 가능한 항목: 이름, 학과, 전화번호, BOJ 아이디, 주 언어")
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/me")
    public ApiResponse<Void> updateMyInfo(@RequestBody @Valid MemberUpdateRequestDto requestDto) {
        memberService.updateMyInfo(requestDto);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), null);
    }
}
