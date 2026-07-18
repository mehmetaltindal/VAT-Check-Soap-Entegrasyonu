package com.example.viesinvoice.service;

import com.example.viesinvoice.model.Invoice;
import com.example.viesinvoice.model.InvoiceValidationResult;

public interface InvoiceValidationService {

    InvoiceValidationResult validate(Invoice invoice);
}
