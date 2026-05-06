package com.solutio.api.domain.blacklist.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class BlacklistAddRequestDto {
    @NotBlank(message = "학번은 비어있을 수 없습니다.")
    private String studentId;

    @NotBlank(message = "블랙 리스트 사유는 비어있을 수 없습니다.")
    private String reason;
}
