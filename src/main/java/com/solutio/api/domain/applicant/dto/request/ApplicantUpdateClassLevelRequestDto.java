package com.solutio.api.domain.applicant.dto.request;

import com.solutio.api.domain.member.domain.ClassLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class ApplicantUpdateClassLevelRequestDto {
    @Schema(description = "배정반",example = "SEED")
    private ClassLevel classLevel;
}
