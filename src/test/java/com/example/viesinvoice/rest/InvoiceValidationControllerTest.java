package com.example.viesinvoice.rest;

import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.model.Invoice;
import com.example.viesinvoice.model.InvoiceValidationResult;
import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.model.VatIdentifier;
import com.example.viesinvoice.service.InvoiceValidationService;
import com.example.viesinvoice.util.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvoiceValidationControllerTest {

    private InvoiceValidationService service;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(InvoiceValidationService.class);
        InvoiceValidationController controller = new InvoiceValidationController(service);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void normalizesRequestAndReturnsSuccessfulDecision() throws Exception {
        VatIdentifier seller = new VatIdentifier("DE", "129273398");
        VatIdentifier buyer = new VatIdentifier("FR", "40303265045");
        when(service.validate(any())).thenReturn(new InvoiceValidationResult(
                "INV-1",
                true,
                GeneralConstants.INVOICE_ISSUABLE_MESSAGE,
                checked(seller, true),
                checked(buyer, true)
        ));

        mockMvc.perform(post("/api/v1/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": " INV-1 ",
                                  "seller": {"countryCode": " de ", "vatNumber": " 129273398 "},
                                  "buyer": {"countryCode": "fr", "vatNumber": "40303265045"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuable").value(true))
                .andExpect(jsonPath("$.seller.status").value("VALID"))
                .andExpect(jsonPath("$.buyer.status").value("VALID"));

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        org.mockito.Mockito.verify(service).validate(captor.capture());
        assertThat(captor.getValue().invoiceNumber()).isEqualTo("INV-1");
        assertThat(captor.getValue().seller()).isEqualTo(seller);
    }

    @Test
    void returnsNotCheckedBuyerForInvalidSeller() throws Exception {
        VatIdentifier seller = new VatIdentifier("DE", "000000000");
        VatIdentifier buyer = new VatIdentifier("FR", "40303265045");
        when(service.validate(any())).thenReturn(new InvoiceValidationResult(
                "INV-1",
                false,
                GeneralConstants.SELLER_INVALID_MESSAGE,
                checked(seller, false),
                PartyValidationResult.notChecked(buyer)
        ));

        mockMvc.perform(validRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuable").value(false))
                .andExpect(jsonPath("$.buyer.status").value("NOT_CHECKED"))
                .andExpect(jsonPath("$.buyer.name").doesNotExist());
    }

    @Test
    void rejectsMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceNumber": "",
                                  "seller": {"countryCode": "DEU", "vatNumber": ""},
                                  "buyer": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorConstants.VALIDATION_ERROR_CODE));
    }

    @Test
    void mapsApiExceptionToItsHttpStatus() throws Exception {
        when(service.validate(any())).thenThrow(new ApiException(
                HttpStatus.GATEWAY_TIMEOUT,
                ErrorConstants.TIMEOUT_CODE,
                ErrorConstants.TIMEOUT_MESSAGE
        ));

        mockMvc.perform(validRequest())
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.errorCode").value(ErrorConstants.TIMEOUT_CODE))
                .andExpect(jsonPath("$.message").value(ErrorConstants.TIMEOUT_MESSAGE));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validRequest()
            throws Exception {
        return post("/api/v1/invoices/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "invoiceNumber", "INV-1",
                        "seller", java.util.Map.of("countryCode", "DE", "vatNumber", "129273398"),
                        "buyer", java.util.Map.of("countryCode", "FR", "vatNumber", "40303265045")
                )));
    }

    private PartyValidationResult checked(VatIdentifier identifier, boolean valid) {
        return PartyValidationResult.checked(identifier, valid, "2026-07-18", "Company", "Address");
    }
}
