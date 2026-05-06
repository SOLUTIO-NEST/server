package com.solutio.api.domain.member.dto.response;

import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;

public record MemberMyInfoResponseDto(
        String studentId,
        String email,
        String role,
        String classLevel,
        String name,
        String department,
        String phoneNumber,
        String BojId,
        MainLanguage mainLanguage
) {
    public static MemberMyInfoResponseDto from(Member member) {
        return new MemberMyInfoResponseDto(
                member.getStudentId(),
                member.getEmail(),
                member.getRole().getDescription(),
                member.getClassLevel() == null ? null : member.getClassLevel().getDescription(),
                member.getName(),
                member.getDepartment(),
                member.getPhoneNumber(),
                member.getBojId(),
                member.getMainLanguage()
        );
    }
}
