package com.solutio.api.domain.recruitment.dto.response;

import com.solutio.api.domain.recruitment.domain.Recruitment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class RecruitmentResponseDto {

    private Long id;
    private String title;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public static RecruitmentResponseDto from(Recruitment recruitment) {
        return RecruitmentResponseDto.builder()
            .id(recruitment.getId())
            .title(recruitment.getTitle())
            .startDateTime(recruitment.getStartDateTime())
            .endDateTime(recruitment.getEndDateTime())
            .build();
    }
}
