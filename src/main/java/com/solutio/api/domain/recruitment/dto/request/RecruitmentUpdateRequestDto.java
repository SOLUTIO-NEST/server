package com.solutio.api.domain.recruitment.dto.request;

import com.solutio.api.domain.recruitment.domain.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentUpdateRequestDto {

    @Schema(description = "제목", example = "2기 모집")
    private String title;

    @Schema(description = "모집 시작일", example = "2026-02-16T00:00:00")
    private LocalDateTime startDateTime;

    @Schema(description = "모집 마감일", example = "2026-03-14T00:00:00")
    private LocalDateTime endDateTime;

    @Schema(description = "최종 발표일", example = "2026-03-20T00:00:00")
    private LocalDateTime announcementDateTime;

    @Schema(description = "모집 상태", example = "OPEN")
    private RecruitmentStatus status;

    @Schema(description = "합격 메시지", example = "축하드립니다!")
    private String passedMessage;
}