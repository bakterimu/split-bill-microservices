package com.rezeptor.settlement.svc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;

    public static <T> Response<T> ok(T data) {
        return Response.<T>builder()
                .success(true)
                .message("Request successful")
                .data(data)
                .statusCode(200)
                .build();
    }

    public static <T> Response<T> created(T data) {
        return Response.<T>builder()
                .success(true)
                .message("Resource created successfully")
                .data(data)
                .statusCode(201)
                .build();
    }

    public static <T> Response<T> badRequest(T problem) {
        return Response.<T>builder()
                .success(false)
                .message("Bad request")
                .statusCode(400)
                .data(problem)
                .build();
    }

    public static <T> Response<T> notFound(T problem) {
        return Response.<T>builder()
                .success(false)
                .message("Resource not found")
                .statusCode(404)
                .data(problem)
                .build();
    }

    public static <T> Response<T> unprocessableEntity(T problem) {
        return Response.<T>builder()
                .success(false)
                .message("Business rule violation")
                .statusCode(422)
                .data(problem)
                .build();
    }

    public static <T> Response<T> serviceUnavailable(T problem) {
        return Response.<T>builder()
                .success(false)
                .message("Service unavailable")
                .statusCode(503)
                .data(problem)
                .build();
    }

    public static <T> Response<T> internalServerError(T problem) {
        return Response.<T>builder()
                .success(false)
                .message("Internal server error")
                .statusCode(500)
                .data(problem)
                .build();
    }

    public ResponseEntity<Response<T>> toEntity() {
        return ResponseEntity.status(statusCode).body(this);
    }
}
