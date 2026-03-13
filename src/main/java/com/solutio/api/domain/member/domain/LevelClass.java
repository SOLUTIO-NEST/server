package com.solutio.api.domain.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LevelClass {
    SEED("SEED","Seed"),
    BRANCH("BRANCH", "Branch"),
    TREE("TREE", "Tree"),
    ;

    private final String key;
    private final String description;
}
