package com.solutio.api.domain.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LevelClass {
    SEED("SEED","시드반"),
    BRANCH("BRANCH", "브랜치반"),
    TREE("TREE", "트리반"),
    ;

    private final String key;
    private final String description;
}
