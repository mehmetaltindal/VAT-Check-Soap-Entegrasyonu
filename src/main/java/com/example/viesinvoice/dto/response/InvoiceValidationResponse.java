package com.example.viesinvoice.dto.response;

public record InvoiceValidationResponse(
        String invoiceNumber,
        boolean issuable,
        String message,
        PartyValidationResponse seller,
        PartyValidationResponse buyer
) {
}
