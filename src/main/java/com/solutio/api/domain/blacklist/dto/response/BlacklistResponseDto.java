package com.solutio.api.domain.blacklist.dto.response;

import com.solutio.api.domain.blacklist.domain.Blacklist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BlacklistResponseDto {

    private final Long id;
    private final String studentId;
    private final String name;
    private final String department;
    private final LocalDateTime createdAt;

    public static BlacklistResponseDto from(Blacklist blacklist) {
        return BlacklistResponseDto.builder()
            .id(blacklist.getId())
            .studentId(blacklist.getStudentId())
            .name(blacklist.getName())
            .department(blacklist.getDepartment())
            .createdAt(blacklist.getCreatedAt())
            .build();
    }
}
