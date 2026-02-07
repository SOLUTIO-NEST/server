package com.solutio.api.global.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Builder
@Getter
public class Body {

    private HttpStatus httpStatus;

    private final boolean isSuccess;

    private final String code;

    private final String message;

}
