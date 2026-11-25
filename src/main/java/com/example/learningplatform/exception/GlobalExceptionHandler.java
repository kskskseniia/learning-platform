package com.example.learningplatform.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(InvalidResourceStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidResourceStateException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler({
            DuplicateResourceException.class,
            ResourceInUseException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiError> handleConflict(RuntimeException exception) {
        String message = exception instanceof DuplicateResourceException
                        || exception instanceof ResourceInUseException
                ? exception.getMessage()
                : "Resource conflicts with existing data";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest() {
        return ResponseEntity.badRequest()
                .body(new ApiError("Malformed or unsupported request body"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ValidationError("Validation failed", errors));
    }
}
