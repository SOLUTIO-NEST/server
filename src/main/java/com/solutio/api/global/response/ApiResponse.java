package com.solutio.api.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonPropertyOrder({"success", "status", "message", "data"})
public class ApiResponse<T> {

    private final boolean isSuccess;

    private final String status;

    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static <T> ApiResponse<T> success(String status, String message, T data) {
        return new ApiResponse<>(true, status, message, data);
    }

    public static <T> ApiResponse<T> onFailure(String status, String message, T data) {
        return  new ApiResponse<>(false, status, message, data);
    }

}

