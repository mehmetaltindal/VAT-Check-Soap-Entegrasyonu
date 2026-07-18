package com.example.viesinvoice.constants;

public final class ErrorConstants {

    public static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
    public static final String VALIDATION_ERROR_MESSAGE = "İstek doğrulaması başarısız";

    public static final String INVALID_INPUT_CODE = "VIES_INVALID_INPUT";
    public static final String INVALID_INPUT_MESSAGE = "VIES, gönderilen KDV numarasını kabul etmedi";

    public static final String SERVICE_UNAVAILABLE_CODE = "VIES_SERVICE_UNAVAILABLE";
    public static final String SERVICE_UNAVAILABLE_MESSAGE = "VIES servisine geçici olarak ulaşılamıyor";

    public static final String TIMEOUT_CODE = "VIES_TIMEOUT";
    public static final String TIMEOUT_MESSAGE = "VIES servisi belirlenen süre içinde yanıt vermedi";

    public static final String UPSTREAM_ERROR_CODE = "VIES_UPSTREAM_ERROR";
    public static final String UPSTREAM_ERROR_MESSAGE = "VIES servisinden beklenmeyen bir yanıt alındı";

    public static final String REQUEST_SCHEMA_ERROR_CODE = "SOAP_REQUEST_SCHEMA_INVALID";
    public static final String REQUEST_SCHEMA_ERROR_MESSAGE = "Oluşturulan SOAP isteği VIES şemasına uygun değil";

    public static final String RESPONSE_SCHEMA_ERROR_CODE = "SOAP_RESPONSE_SCHEMA_INVALID";
    public static final String RESPONSE_SCHEMA_ERROR_MESSAGE = "VIES yanıtı servis şemasına uygun değil";

    public static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    public static final String INTERNAL_ERROR_MESSAGE = "Beklenmeyen bir hata oluştu";

    private ErrorConstants() {
    }
}
