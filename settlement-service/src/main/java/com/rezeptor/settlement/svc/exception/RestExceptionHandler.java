package com.rezeptor.settlement.svc.exception;

import com.rezeptor.settlement.svc.data.ValidationErrorData;
import com.rezeptor.settlement.svc.dto.response.Response;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return Response.notFound(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage())).toEntity();
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return Response.unprocessableEntity(
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage())).toEntity();
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<?> handleServiceUnavailableException(ServiceUnavailableException ex) {
        log.warn("Dependency unavailable: {}", ex.getMessage());
        return Response.serviceUnavailable(
                ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage())).toEntity();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<ValidationErrorData> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ValidationErrorData(err.getField(), err.getDefaultMessage()))
                .toList();
        log.warn("Validation error: {}", errors);
        return validationProblem(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        List<ValidationErrorData> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(err -> new ValidationErrorData(
                                result.getMethodParameter().getParameterName(), err.getDefaultMessage())))
                .toList();
        log.warn("Method validation error: {}", errors);
        return validationProblem(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("Internal server error", ex);
        return Response.internalServerError(
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
                .toEntity();
    }

    private ResponseEntity<?> validationProblem(List<ValidationErrorData> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
        problem.setProperty("errors", errors);
        return Response.badRequest(problem).toEntity();
    }
}
