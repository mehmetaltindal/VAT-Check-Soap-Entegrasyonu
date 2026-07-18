package com.example.viesinvoice.dto.response;

public record ErrorResponse(
        String errorCode,
        String message
) {
}
