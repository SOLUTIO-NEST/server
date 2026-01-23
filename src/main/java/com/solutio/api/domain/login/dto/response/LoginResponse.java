package com.solutio.api.domain.login.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginResponse {

    private final String header;

    private final boolean body;

    public static LoginResponse create(String header, boolean body) {
        return new LoginResponse(header, body);
    }

    public boolean getBody() {
        return body;
    }
}
