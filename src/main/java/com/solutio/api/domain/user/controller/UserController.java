package com.solutio.api.domain.user.controller;

import com.solutio.api.domain.user.dto.response.UserMyInfoResponseDto;
import com.solutio.api.domain.user.service.UserService;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 공통")
public class UserController {
    private final UserService userService;

    @Operation(summary = "[Guest] 내 정보 조회", description = "ROLE_GUEST 이상의 권한이 필요함")
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/me")
    public ApiResponse<UserMyInfoResponseDto> getMyInfo() {
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), userService.getMyInfo());
    }
}
