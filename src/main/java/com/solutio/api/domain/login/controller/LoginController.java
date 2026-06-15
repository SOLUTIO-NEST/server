package com.solutio.api.domain.login.controller;

import com.solutio.api.domain.login.dto.request.LoginRequestDto;
import com.solutio.api.domain.login.dto.response.TokenInfo;
import com.solutio.api.domain.login.service.LoginService;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/login")
@RequiredArgsConstructor
@Tag(name = "Login", description = "로그인")
public class LoginController {

    private final LoginService loginService;

    @Operation(summary = "로그인", description = "ROLE_ANONYMOUS 권한이 필요함")
    @PostMapping("")
    public ApiResponse<TokenInfo> login(
        @Valid @RequestBody LoginRequestDto requestDto
    ) {
        TokenInfo tokenInfo = loginService.login(requestDto);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), tokenInfo);
    }

    @Operation(summary = "[Guest] 토큰 재발급", description = "ROLE_GUEST 이상의 권한이 필요함")
    @PostMapping("/reissue")
    @PreAuthorize("hasRole('GUEST')")
    public ApiResponse<TokenInfo> reissueToken(
        HttpServletRequest request
    ) {
        TokenInfo tokenInfo = loginService.reissueToken(request);
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), tokenInfo);
    }
}
