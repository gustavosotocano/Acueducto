package com.acueducto.veredal.shared.api;

import com.acueducto.veredal.shared.multitenancy.TenantRequiredException;
import com.acueducto.veredal.shared.multitenancy.TenantNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TenantRequiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTenantRequired(TenantRequiredException ex, HttpServletRequest request) {
        return new ApiError(OffsetDateTime.now(), 400, "Bad Request", "TENANT_REQUIRED: " + ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TenantNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTenantNotFound(TenantNotFoundException ex, HttpServletRequest request) {
        return new ApiError(OffsetDateTime.now(), 400, "Bad Request", "TENANT_NOT_FOUND: " + ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return new ApiError(OffsetDateTime.now(), 400, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return new ApiError(OffsetDateTime.now(), 400, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Validation error");
        return new ApiError(OffsetDateTime.now(), 400, "Bad Request", message, request.getRequestURI());
    }
}
