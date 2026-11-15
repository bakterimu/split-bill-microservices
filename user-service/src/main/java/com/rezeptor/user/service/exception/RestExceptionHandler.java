package com.rezeptor.user.service.exception;

import com.rezeptor.user.service.dto.response.Response;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";

    public record ValidationError(String field, String message) {
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return Response.notFound(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage())).toEntity();
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<?> handleEntityExistsException(EntityExistsException ex) {
        log.warn("Entity already exists: {}", ex.getMessage());
        return Response.badRequest(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage())).toEntity();
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = fromBindingResult(ex.getBindingResult()).toList();
        log.warn("Validation error: {}", errors);
        return validationProblem(errors);
    }


    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        List<ValidationError> errors = ex.getParameterValidationResults().stream()
                .flatMap(this::fromParameterResult)
                .toList();
        log.warn("Method validation error: {}", errors);
        return validationProblem(errors);
    }

    @ExceptionHandler(Exception.class)
    public Response<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Internal server error", ex);
        return Response.internalServerError(
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }

    private Stream<ValidationError> fromBindingResult(BindingResult bindingResult) {
        Stream<ValidationError> globalErrors = bindingResult.getGlobalErrors().stream()
                .map(err -> new ValidationError(err.getObjectName(), err.getDefaultMessage()));
        Stream<ValidationError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(err -> new ValidationError(err.getField(), err.getDefaultMessage()));
        return Stream.concat(globalErrors, fieldErrors);
    }

    private Stream<ValidationError> fromParameterResult(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            Stream<ValidationError> globalErrors = parameterErrors.getGlobalErrors().stream()
                    .map(err -> new ValidationError(err.getObjectName(), err.getDefaultMessage()));
            Stream<ValidationError> fieldErrors = parameterErrors.getFieldErrors().stream()
                    .map(err -> new ValidationError(err.getField(), err.getDefaultMessage()));
            return Stream.concat(globalErrors, fieldErrors);
        }

        String parameterName = Objects.requireNonNullElse(
                result.getMethodParameter().getParameterName(),
                "arg" + result.getMethodParameter().getParameterIndex());

        return result.getResolvableErrors().stream()
                .map(err -> new ValidationError(parameterName, err.getDefaultMessage()));
    }

    private ResponseEntity<?> validationProblem(List<ValidationError> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
        problem.setProperty("errors", errors);
        return Response.badRequest(problem).toEntity();
    }
}
