package com.example.viesinvoice.model;

public record PartyValidationResult(
        ValidationStatus status,
        String countryCode,
        String vatNumber,
        String requestDate,
        String name,
        String address
) {

    public static PartyValidationResult checked(
            VatIdentifier vatIdentifier,
            boolean valid,
            String requestDate,
            String name,
            String address
    ) {
        return new PartyValidationResult(
                valid ? ValidationStatus.VALID : ValidationStatus.INVALID,
                vatIdentifier.countryCode(),
                vatIdentifier.vatNumber(),
                requestDate,
                name,
                address
        );
    }

    public static PartyValidationResult notChecked(VatIdentifier vatIdentifier) {
        return new PartyValidationResult(
                ValidationStatus.NOT_CHECKED,
                vatIdentifier.countryCode(),
                vatIdentifier.vatNumber(),
                null,
                null,
                null
        );
    }

    public boolean isValid() {
        return status == ValidationStatus.VALID;
    }
}
