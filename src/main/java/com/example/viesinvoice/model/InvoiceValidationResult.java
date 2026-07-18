package com.example.viesinvoice.model;

public record InvoiceValidationResult(
        String invoiceNumber,
        boolean issuable,
        String message,
        PartyValidationResult seller,
        PartyValidationResult buyer
) {
}
