package com.solutio.api.domain.login.controller;

import com.solutio.api.domain.login.dto.request.LoginRequestDto;
import com.solutio.api.domain.login.dto.response.LoginResponse;
import com.solutio.api.domain.login.dto.response.TokenHeader;
import com.solutio.api.domain.login.service.LoginService;
import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/login")
@Tag(name = "Login", description = "로그인")
public class LoginController {

    private final LoginService loginService;

    private final String authHeader;

    public LoginController(
        LoginService loginService,
        @Value("${security.auth.header}") String authHeader) {

        this.loginService = loginService;
        this.authHeader = authHeader;
    }

    @Operation(summary = "로그인", description = "ROLE_ANONYMOUS 권한이 필요함")
    @PostMapping("")
    public ApiResponse<?> login(
        HttpServletRequest request,
        HttpServletResponse response,
        @Valid @RequestBody LoginRequestDto requestDto
    ) {
        LoginResponse result = loginService.login(requestDto);
        response.setHeader(authHeader, result.getHeader());
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), result.getBody());
    }

    @Operation(summary = "[Guest] 토큰 재발급", description = "ROLE_GUEST 이상의 권한이 필요함")
    @PostMapping("/reissue")
    @PreAuthorize("hasRole('GUEST')")
    public ApiResponse<Void> reissueToken(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        TokenHeader headerData = loginService.reissueToken(request);
        response.setHeader(authHeader, headerData.toJson());
        return ApiResponse.success(Status.OK.getCode(), Status.OK.getMessage(), null);
    }
}