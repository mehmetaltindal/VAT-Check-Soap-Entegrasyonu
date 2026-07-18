package com.example.viesinvoice.service.impl;

import com.example.viesinvoice.model.Invoice;
import com.example.viesinvoice.model.InvoiceValidationResult;
import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.model.ValidationStatus;
import com.example.viesinvoice.model.VatIdentifier;
import com.example.viesinvoice.service.ViesClientService;
import com.example.viesinvoice.util.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceValidationServiceImplTest {

    private final VatIdentifier seller = new VatIdentifier("DE", "129273398");
    private final VatIdentifier buyer = new VatIdentifier("FR", "40303265045");
    private final Invoice invoice = new Invoice("INV-1", seller, buyer);

    private ViesClientService viesClient;
    private InvoiceValidationServiceImpl service;

    @BeforeEach
    void setUp() {
        viesClient = mock(ViesClientService.class);
        service = new InvoiceValidationServiceImpl(viesClient);
    }

    @Test
    void returnsIssuableWhenBothPartiesAreValid() {
        when(viesClient.validate(seller)).thenReturn(valid(seller));
        when(viesClient.validate(buyer)).thenReturn(valid(buyer));

        InvoiceValidationResult result = service.validate(invoice);

        assertThat(result.issuable()).isTrue();
        assertThat(result.seller().status()).isEqualTo(ValidationStatus.VALID);
        assertThat(result.buyer().status()).isEqualTo(ValidationStatus.VALID);
    }

    @Test
    void stopsAfterInvalidSellerAndMarksBuyerNotChecked() {
        when(viesClient.validate(seller)).thenReturn(invalid(seller));

        InvoiceValidationResult result = service.validate(invoice);

        assertThat(result.issuable()).isFalse();
        assertThat(result.seller().status()).isEqualTo(ValidationStatus.INVALID);
        assertThat(result.buyer().status()).isEqualTo(ValidationStatus.NOT_CHECKED);
        verify(viesClient, never()).validate(buyer);
    }

    @Test
    void returnsNotIssuableWhenBuyerIsInvalid() {
        when(viesClient.validate(seller)).thenReturn(valid(seller));
        when(viesClient.validate(buyer)).thenReturn(invalid(buyer));

        InvoiceValidationResult result = service.validate(invoice);

        assertThat(result.issuable()).isFalse();
        assertThat(result.buyer().status()).isEqualTo(ValidationStatus.INVALID);
    }

    @Test
    void propagatesTechnicalFailuresAndDoesNotCallBuyer() {
        ApiException failure = new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CODE", "failure");
        when(viesClient.validate(seller)).thenThrow(failure);

        assertThatThrownBy(() -> service.validate(invoice)).isSameAs(failure);
        verify(viesClient, never()).validate(buyer);
    }

    private PartyValidationResult valid(VatIdentifier identifier) {
        return PartyValidationResult.checked(identifier, true, "2026-07-18", "Company", "Address");
    }

    private PartyValidationResult invalid(VatIdentifier identifier) {
        return PartyValidationResult.checked(identifier, false, "2026-07-18", "---", "---");
    }
}
