package com.example.viesinvoice.model;

public record VatIdentifier(
        String countryCode,
        String vatNumber
) {
}
