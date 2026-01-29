package com.solutio.api.domain.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecruitmentUpdateRequestDto {

    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    private String title;

    @NotNull(message = "시작 일시는 필수 값입니다.")
    private LocalDateTime startDateTime;

    @NotNull(message = "종료 일시는 필수 값입니다.")
    private LocalDateTime endDateTime;
}