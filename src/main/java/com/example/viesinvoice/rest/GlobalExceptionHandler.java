package com.example.viesinvoice.rest;

import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.dto.response.ErrorResponse;
import com.example.viesinvoice.util.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        LOGGER.warn("Request failed with error code {}: {}", exception.getErrorCode(), exception.getMessage());
        return ResponseEntity
                .status(exception.getHttpStatus())
                .body(new ErrorResponse(exception.getErrorCode(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        ErrorConstants.VALIDATION_ERROR_CODE,
                        ErrorConstants.VALIDATION_ERROR_MESSAGE
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        LOGGER.error("Unexpected request failure", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorConstants.INTERNAL_ERROR_CODE,
                        ErrorConstants.INTERNAL_ERROR_MESSAGE
                ));
    }
}
