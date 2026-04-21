package com.solutio.api.domain.blacklist.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class BlacklistUpdateReasonRequestDto {

    @NotBlank(message = "블랙리스트 사유는 비어있을 수 없습니다.")
    private String reason;
}
