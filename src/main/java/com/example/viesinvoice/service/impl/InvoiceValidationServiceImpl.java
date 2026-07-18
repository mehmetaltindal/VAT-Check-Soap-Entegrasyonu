package com.example.viesinvoice.service.impl;

import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.model.Invoice;
import com.example.viesinvoice.model.InvoiceValidationResult;
import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.service.InvoiceValidationService;
import com.example.viesinvoice.service.ViesClientService;
import org.springframework.stereotype.Service;

@Service
public class InvoiceValidationServiceImpl implements InvoiceValidationService {

    private final ViesClientService viesClient;

    public InvoiceValidationServiceImpl(ViesClientService viesClient) {
        this.viesClient = viesClient;
    }

    @Override
    public InvoiceValidationResult validate(Invoice invoice) {
        PartyValidationResult sellerResult = viesClient.validate(invoice.seller());
        if (!sellerResult.isValid()) {
            return new InvoiceValidationResult(
                    invoice.invoiceNumber(),
                    false,
                    GeneralConstants.SELLER_INVALID_MESSAGE,
                    sellerResult,
                    PartyValidationResult.notChecked(invoice.buyer())
            );
        }

        PartyValidationResult buyerResult = viesClient.validate(invoice.buyer());
        if (!buyerResult.isValid()) {
            return new InvoiceValidationResult(
                    invoice.invoiceNumber(),
                    false,
                    GeneralConstants.BUYER_INVALID_MESSAGE,
                    sellerResult,
                    buyerResult
            );
        }

        return new InvoiceValidationResult(
                invoice.invoiceNumber(),
                true,
                GeneralConstants.INVOICE_ISSUABLE_MESSAGE,
                sellerResult,
                buyerResult
        );
    }
}
