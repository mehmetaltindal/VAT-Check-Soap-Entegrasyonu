package com.example.viesinvoice.helper;

import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.util.ApiException;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorHelperTest {

    private final ValidatorHelper validatorHelper = new ValidatorHelper();

    @Test
    void acceptsSchemaCompliantRequestAndResponse() {
        assertThatCode(() -> validatorHelper.validateRequest(source("""
                <checkVat xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                  <countryCode>DE</countryCode>
                  <vatNumber>129273398</vatNumber>
                </checkVat>
                """))).doesNotThrowAnyException();

        assertThatCode(() -> validatorHelper.validateResponse(source("""
                <checkVatResponse xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                  <countryCode>DE</countryCode>
                  <vatNumber>129273398</vatNumber>
                  <requestDate>2026-07-18</requestDate>
                  <valid>true</valid>
                  <name>Example GmbH</name>
                  <address>Berlin</address>
                </checkVatResponse>
                """))).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidOutboundPayloadAsInternalError() {
        assertThatThrownBy(() -> validatorHelper.validateRequest(source("""
                <checkVat xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                  <countryCode>DE</countryCode>
                </checkVat>
                """)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.REQUEST_SCHEMA_ERROR_CODE);
                    assertThat(exception.getHttpStatus().value()).isEqualTo(500);
                });
    }

    @Test
    void rejectsInvalidInboundPayloadAsBadGateway() {
        assertThatThrownBy(() -> validatorHelper.validateResponse(source("""
                <checkVatResponse xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                  <countryCode>DE</countryCode>
                  <vatNumber>129273398</vatNumber>
                  <valid>true</valid>
                </checkVatResponse>
                """)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.RESPONSE_SCHEMA_ERROR_CODE);
                    assertThat(exception.getHttpStatus().value()).isEqualTo(502);
                });
    }

    private StreamSource source(String xml) {
        return new StreamSource(new StringReader(xml));
    }
}
