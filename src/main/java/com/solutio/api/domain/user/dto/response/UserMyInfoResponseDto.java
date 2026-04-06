package com.solutio.api.domain.user.dto.response;

import com.solutio.api.domain.applicant.domain.Applicant;
import com.solutio.api.domain.member.domain.MainLanguage;
import com.solutio.api.domain.member.domain.Member;
import com.solutio.api.domain.member.domain.Role;

public record UserMyInfoResponseDto(
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
    public static UserMyInfoResponseDto from(Member member) {
        return new UserMyInfoResponseDto(
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

    public static UserMyInfoResponseDto from(Applicant applicant) {
        return new UserMyInfoResponseDto(
                applicant.getStudentId(),
                applicant.getEmail(),
                Role.GUEST.getDescription(),
                applicant.getClassLevel() == null ? null : applicant.getClassLevel().getDescription(),
                applicant.getName(),
                applicant.getDepartment(),
                applicant.getPhoneNumber(),
                applicant.getBojId(),
                applicant.getMainLanguage()
        );
    }
}
