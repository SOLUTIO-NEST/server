package com.solutio.api.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum Status {

    //공통 정상 응답
    OK(HttpStatus.OK, "COMMON200", "성공입니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "생성되었습니다."),

    //공통 오류 응답
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON409", "이미 생성되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버에 오류가 발생했습니다."),

    //
    APPLICANT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICANT404", "지원자가 존재하지 않습니다."),
    NOT_APPROVED_APPLICATION(HttpStatus.CONFLICT, "APPLICANT409", "승인되지 않은 지원자입니다."),

    //
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUITMENT404", "모집이 존재하지 않습니다."),

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "DATE400", "시작 날짜는 종료 날짜보다 앞설 수 없습니다.")

    ;
    private final HttpStatus httpStatus;

    private final String code;

    private final String message;

    public Body getBody() {
        return Body.builder()
            .message(message)
            .code(code)
            .isSuccess(httpStatus.is2xxSuccessful())
            .httpStatus(httpStatus)
            .build();
    }

}
