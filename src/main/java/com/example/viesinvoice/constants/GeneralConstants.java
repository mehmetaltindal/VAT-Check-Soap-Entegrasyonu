package com.example.viesinvoice.constants;

import java.util.Set;

public final class GeneralConstants {

    public static final String COUNTRY_CODE_REGEX = "^\\s*[A-Za-z]{2}\\s*$";
    public static final String VIES_TYPES_NAMESPACE =
            "urn:ec.europa.eu:taxud:vies:services:checkVat:types";
    public static final String JAXB_CONTEXT_PATH = "com.example.viesinvoice.soap.generated";
    public static final String VIES_XSD_RESOURCE = "xsd/checkVatService.xsd";
    public static final String SOAP_ACTION = "";
    public static final String VIES_RETRY_NAME = "vies";
    public static final String INVOICE_API_PATH = "/api/v1/invoices";
    public static final String VALIDATE_PATH = "/validate";
    public static final String INVOICE_ISSUABLE_MESSAGE = "Fatura kesilebilir";
    public static final String SELLER_INVALID_MESSAGE = "Fatura kesilemez: satıcı KDV numarası geçersiz";
    public static final String BUYER_INVALID_MESSAGE = "Fatura kesilemez: alıcı KDV numarası geçersiz";

    public static final String INVALID_INPUT_FAULT = "INVALID_INPUT";
    public static final Set<String> TRANSIENT_VIES_FAULTS = Set.of(
            "GLOBAL_MAX_CONCURRENT_REQ",
            "MS_MAX_CONCURRENT_REQ",
            "SERVICE_UNAVAILABLE",
            "MS_UNAVAILABLE",
            "TIMEOUT"
    );

    private GeneralConstants() {
    }
}
