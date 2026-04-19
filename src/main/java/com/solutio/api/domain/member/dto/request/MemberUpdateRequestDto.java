package com.solutio.api.domain.member.dto.request;

import com.solutio.api.domain.member.domain.MainLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MemberUpdateRequestDto(
        @NotBlank(message = "이름은 빈 값일 수 없습니다.")
        String name,

        @NotBlank(message = "학과는 빈 값일 수 없습니다.")
        String department,

        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "전화번호는 010-1234-5678 형식으로 입력해야 합니다."
        )
        String phoneNumber,

        @NotBlank(message = "백준 아이디는 빈 값일 수 없습니다.")
        String bojId,

        @NotNull(message = "주 사용 언어는 빈 값일 수 없습니다.")
        MainLanguage mainLanguage
) {
}
