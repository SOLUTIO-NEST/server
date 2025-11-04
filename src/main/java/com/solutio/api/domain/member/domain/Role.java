package com.solutio.api.domain.member.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {

    GUEST("ROLE_GUEST", "준회원"),
    USER("ROLE_USER", "동아리원"),
    STAFF("ROLE_STAFF", "운영진"),
    NEST("ROLE_NEST", "개발자"),
    SUPER("ROLE_SUPER", "관리자")

    ;

    private String key;

    private String description;

}
