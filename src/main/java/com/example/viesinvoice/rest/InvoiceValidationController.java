package com.example.viesinvoice.rest;

import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.dto.request.InvoiceValidationRequest;
import com.example.viesinvoice.dto.request.VatPartyRequest;
import com.example.viesinvoice.dto.response.InvoiceValidationResponse;
import com.example.viesinvoice.dto.response.PartyValidationResponse;
import com.example.viesinvoice.model.Invoice;
import com.example.viesinvoice.model.InvoiceValidationResult;
import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.model.VatIdentifier;
import com.example.viesinvoice.service.InvoiceValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(GeneralConstants.INVOICE_API_PATH)
public class InvoiceValidationController {

    private final InvoiceValidationService invoiceValidationService;

    public InvoiceValidationController(InvoiceValidationService invoiceValidationService) {
        this.invoiceValidationService = invoiceValidationService;
    }

    @PostMapping(GeneralConstants.VALIDATE_PATH)
    public ResponseEntity<InvoiceValidationResponse> validate(
            @Valid @RequestBody InvoiceValidationRequest request
    ) {
        Invoice invoice = toDomain(request);
        return ResponseEntity.ok(toResponse(invoiceValidationService.validate(invoice)));
    }

    private Invoice toDomain(InvoiceValidationRequest request) {
        return new Invoice(
                request.invoiceNumber().trim(),
                toVatIdentifier(request.seller()),
                toVatIdentifier(request.buyer())
        );
    }

    private VatIdentifier toVatIdentifier(VatPartyRequest request) {
        return new VatIdentifier(
                request.countryCode().trim().toUpperCase(Locale.ROOT),
                request.vatNumber().trim()
        );
    }

    private InvoiceValidationResponse toResponse(InvoiceValidationResult result) {
        return new InvoiceValidationResponse(
                result.invoiceNumber(),
                result.issuable(),
                result.message(),
                toPartyResponse(result.seller()),
                toPartyResponse(result.buyer())
        );
    }

    private PartyValidationResponse toPartyResponse(PartyValidationResult result) {
        return new PartyValidationResponse(
                result.status(),
                result.countryCode(),
                result.vatNumber(),
                result.requestDate(),
                result.name(),
                result.address()
        );
    }
}
