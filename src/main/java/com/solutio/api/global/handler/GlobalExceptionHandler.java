package com.solutio.api.global.handler;

import com.solutio.api.global.response.ApiResponse;
import com.solutio.api.global.response.Body;
import com.solutio.api.global.response.GeneralException;
import com.solutio.api.global.response.Status;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpectedException(
        Exception unexpectedException,
        WebRequest webRequest
    ) {
        log.error("예상치 못한 오류 발생: {}", unexpectedException.getMessage());
        log.error("발생 지점: {}", unexpectedException.getStackTrace()[0]);
        Body body = Status.INTERNAL_SERVER_ERROR.getBody();
        ApiResponse<Object> response = ApiResponse.onFailure(body.getCode(), body.getMessage(), null);
        return super.handleExceptionInternal(
            unexpectedException,
            response,
            HttpHeaders.EMPTY,
            HttpStatus.INTERNAL_SERVER_ERROR,
            webRequest
        );
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> exception(
        GeneralException generalException,
        HttpServletRequest request
    ) {
        Body body = generalException.getBody();
        ApiResponse<Object> response = ApiResponse.onFailure(body.getCode(), body.getMessage(), null);
        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(
            generalException,
            response,
            HttpHeaders.EMPTY,
            body.getHttpStatus(),
            webRequest
        );
    }

    @Override // DTO 유효성 검증 시 예외 처리
    public ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        List<Map<String, String>> errors = new ArrayList<>();
        Body body = Status.BAD_REQUEST.getBody();

        exception.getBindingResult().getFieldErrors().forEach(fieldError -> {
            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put("field", fieldError.getField());
            errorDetail.put("message", fieldError.getDefaultMessage());
            errors.add(errorDetail);
        });

        ApiResponse<Object> response = ApiResponse.onFailure(body.getCode(), body.getMessage(), errors);

        return ResponseEntity.status(Status.BAD_REQUEST.getHttpStatus()).body(response);

    }


    @ExceptionHandler(ConstraintViolationException.class) // 파라미터 유효성 검증 시 예외 처리
    public ResponseEntity<Object> handleConstraintViolationException(
        ConstraintViolationException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        Body body = Status.BAD_REQUEST.getBody();

        exception.getConstraintViolations()
            .forEach(violation -> {
                String fieldName = violation.getPropertyPath().toString();
                String errorMessage = violation.getMessage();
                errors.merge(fieldName, errorMessage, (existing, newError) -> existing + ", " + newError);
            });

        ApiResponse<Object> response = ApiResponse.onFailure(body.getCode(), body.getMessage(), errors);
        return ResponseEntity.status(Status.BAD_REQUEST.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> exception(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        Body body = Status.UNAUTHORIZED.getBody();
        ApiResponse<Object> response = ApiResponse.onFailure(body.getCode(), body.getMessage(), null);
        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(
            exception,
            response,
            HttpHeaders.EMPTY,
            body.getHttpStatus(),
            webRequest
        );
    }

}