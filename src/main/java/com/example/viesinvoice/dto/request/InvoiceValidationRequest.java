package com.example.viesinvoice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceValidationRequest(
        @NotBlank(message = "Fatura numarası zorunludur")
        String invoiceNumber,

        @NotNull(message = "Satıcı bilgisi zorunludur")
        @Valid
        VatPartyRequest seller,

        @NotNull(message = "Alıcı bilgisi zorunludur")
        @Valid
        VatPartyRequest buyer
) {
}
