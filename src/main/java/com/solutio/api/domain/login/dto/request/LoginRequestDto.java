package com.solutio.api.domain.login.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    @NotNull
    @Pattern(regexp = "^\\d{9}$", message = "학번은 숫자 9자리여야 합니다.")
    @Schema(description = "학번", example = "202312000", required = true)
    private String id;

    @NotNull
    @Size(max = 128, message = "비밀번호는 최대 128자까지 입력할 수 있습니다.")
    @Schema(description = "비밀번호", example = "Password123!", required = true)
    private String password;
}
