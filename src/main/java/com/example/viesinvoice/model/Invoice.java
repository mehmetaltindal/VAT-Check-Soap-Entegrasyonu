package com.example.viesinvoice.model;

public record Invoice(
        String invoiceNumber,
        VatIdentifier seller,
        VatIdentifier buyer
) {
}
