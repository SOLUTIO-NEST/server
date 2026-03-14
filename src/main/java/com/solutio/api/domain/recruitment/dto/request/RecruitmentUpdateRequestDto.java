package com.solutio.api.domain.recruitment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecruitmentUpdateRequestDto {

    @Schema(description = "제목", example = "2기 모집")
    private String title;

    @Schema(description = "모집 시작일", example = "2026-02-16T00:00:00")
    private LocalDateTime startDateTime;

    @Schema(description = "모집 마감일", example = "2026-03-14T00:00:00")
    private LocalDateTime endDateTime;

    @Schema(description = "합격 메시지", example = "축하드립니다!")
    private String passedMessage;
}