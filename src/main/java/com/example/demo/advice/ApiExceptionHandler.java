package com.example.demo.advice;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.api.response.ApiErrorResponse;
import com.example.demo.api.response.ValidationErrorResponse;
import com.example.demo.service.exception.ScheduleConflictException;
import com.example.demo.service.exception.ScheduleEntryNotFoundException;
import com.example.demo.service.exception.ScheduleSheetNotFoundException;

@RestControllerAdvice(basePackages = "com.example.demo.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(ScheduleSheetNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleScheduleSheetNotFound(
            ScheduleSheetNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Schedule Sheet Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ScheduleEntryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleScheduleEntryNotFound(
            ScheduleEntryNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Schedule Entry Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleScheduleConflict(
            ScheduleConflictException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Schedule Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse.FieldErrorDetail> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::toFieldErrorDetail)
                        .toList();

        ValidationErrorResponse response = new ValidationErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "入力値が不正です",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "予期しないエラーが発生しました",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    private ValidationErrorResponse.FieldErrorDetail toFieldErrorDetail(FieldError error) {
        return new ValidationErrorResponse.FieldErrorDetail(
                error.getField(),
                error.getDefaultMessage()
        );
    }
}