package com.example.viesinvoice.dto.response;

import com.example.viesinvoice.model.ValidationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartyValidationResponse(
        ValidationStatus status,
        String countryCode,
        String vatNumber,
        String requestDate,
        String name,
        String address
) {
}
