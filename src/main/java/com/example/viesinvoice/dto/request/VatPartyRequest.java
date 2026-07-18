package com.example.viesinvoice.dto.request;

import com.example.viesinvoice.constants.GeneralConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VatPartyRequest(
        @NotBlank(message = "Ülke kodu zorunludur")
        @Pattern(regexp = GeneralConstants.COUNTRY_CODE_REGEX, message = "Ülke kodu tam olarak iki harf içermelidir")
        String countryCode,

        @NotBlank(message = "KDV numarası zorunludur")
        String vatNumber
) {
}
