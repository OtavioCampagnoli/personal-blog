package com.otavio.blog.application.api.exception;

import com.otavio.blog.shared.domain.DomainException;
import com.otavio.blog.shared.domain.DomainValidationException;
import com.otavio.blog.shared.domain.ForbiddenException;
import com.otavio.blog.shared.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler global para exceções, seguindo RFC 7807 (Problem Details)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(NotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .code(ex.getCode())
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .code(ex.getCode())
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ValidationProblemDetail> handleDomainValidationException(
            DomainValidationException ex, WebRequest request) {
        
        List<ValidationError> errors = ex.getErrors().stream()
            .map(error -> new ValidationError(
                error.field(),
                error.code(),
                error.message(),
                error.providedValue(),
                error.example()
            ))
            .toList();

        ValidationProblemDetail problem = ValidationProblemDetail.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .code("VALIDATION_ERRORS")
            .message("Validation errors in the provided fields")
            .errors(errors)
            .build();

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        List<ValidationError> errors = new ArrayList<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(new ValidationError(
                error.getField(),
                error.getCode(),
                error.getDefaultMessage(),
                error.getRejectedValue(),
                null
            ));
        });

        ValidationProblemDetail problem = ValidationProblemDetail.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .code("VALIDATION_ERRORS")
            .message("Validation errors in the provided fields")
            .errors(errors)
            .build();

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .code(ex.getCode())
            .message(ex.getMessage())
            .build();

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
        ex.printStackTrace(); // Log for debugging
        
        ProblemDetail problem = ProblemDetail.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .code("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // DTOs for error responses (RFC 7807)
    
    public record ProblemDetail(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, Object> details
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Instant timestamp;
            private int status;
            private String error;
            private String code;
            private String message;
            private Map<String, Object> details;

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder code(String code) {
                this.code = code;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder details(Map<String, Object> details) {
                this.details = details;
                return this;
            }

            public ProblemDetail build() {
                return new ProblemDetail(timestamp, status, error, code, message, details);
            }
        }
    }

    public record ValidationProblemDetail(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        List<ValidationError> errors
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Instant timestamp;
            private int status;
            private String error;
            private String code;
            private String message;
            private List<ValidationError> errors;

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder code(String code) {
                this.code = code;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder errors(List<ValidationError> errors) {
                this.errors = errors;
                return this;
            }

            public ValidationProblemDetail build() {
                return new ValidationProblemDetail(timestamp, status, error, code, message, errors);
            }
        }
    }

    public record ValidationError(
        String field,
        String code,
        String message,
        Object providedValue,
        String example
    ) {}
}
