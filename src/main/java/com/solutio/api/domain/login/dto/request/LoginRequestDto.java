package com.solutio.api.domain.login.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    @NotNull
    @Schema(description = "학번", example = "202312000", required = true)
    private String id;

    @NotNull
    @Schema(description = "비밀번호", example = "1234", required = true)
    private String password;
}
