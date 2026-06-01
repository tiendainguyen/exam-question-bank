package com.exambank.common;

/**
 * Uniform API envelope. {@code success=true} carries {@code data};
 * {@code success=false} carries {@code error}. Errors surfaced via
 * {@link GlobalExceptionHandler} use RFC 7807 ProblemDetail instead.
 */
public record ApiResponse<T>(boolean success, T data, String error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String error) {
        return new ApiResponse<>(false, null, error);
    }
}
