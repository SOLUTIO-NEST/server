package com.solutio.api.domain.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClassLevel {
    UNASSIGNED("UNASSIGNED", "미배정"),
    SEED("SEED","Seed"),
    BRANCH("BRANCH", "Branch"),
    TREE("TREE", "Tree"),
    ;

    private final String key;
    private final String description;
}
