package com.solutio.api.domain.blacklist.dto.response;

import com.solutio.api.domain.blacklist.domain.Blacklist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BlacklistDetailResponseDto {

    private final Long id;
    private final String studentId;
    private final String name;
    private final String email;
    private final String department;
    private final String phoneNumber;
    private final String reason;
    private final LocalDateTime createdAt;

    public static BlacklistDetailResponseDto from(Blacklist blacklist) {
        return BlacklistDetailResponseDto.builder()
            .id(blacklist.getId())
            .studentId(blacklist.getStudentId())
            .name(blacklist.getName())
            .email(blacklist.getEmail())
            .department(blacklist.getDepartment())
            .phoneNumber(blacklist.getPhoneNumber())
            .reason(blacklist.getReason())
            .createdAt(blacklist.getCreatedAt())
            .build();
    }
}
