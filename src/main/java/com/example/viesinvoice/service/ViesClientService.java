package com.example.viesinvoice.service;

import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.model.VatIdentifier;

public interface ViesClientService {

    PartyValidationResult validate(VatIdentifier vatIdentifier);
}
