package com.solutio.api.domain.applicant.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PassStatus {
    PENDING("PENDING", "미정"),
    APPROVED("APPROVED", "합격"),
    REJECTED("REJECTED", "불합격");

    private final String key;
    private final String description;
}
