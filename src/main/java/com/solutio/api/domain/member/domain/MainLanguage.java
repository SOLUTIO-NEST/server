package com.solutio.api.domain.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MainLanguage {

    C("C", "C"),
    CPP("C++", "C++"),
    JAVA("Java", "Java"),
    PYTHON("Python", "Python"),
    JAVASCRIPT("JavaScript", "JavaScript");

    private final String key;
    private final String description;
}
