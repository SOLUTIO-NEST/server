package com.solutio.api.domain.test.controller;

import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "Test", description = "테스트 api")
public class TestController {

    @PostMapping("/")
    @Operation(summary = "post method 테스트 API", description = "post method 테스트 API입니다.")
    public ApiResponse<?> postTest() {
        return ApiResponse.success(Status.OK.getCode(),
            Status.OK.getMessage(), "성공");
    }

}
