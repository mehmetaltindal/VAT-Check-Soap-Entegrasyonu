package com.example.viesinvoice.service.impl;

import com.example.viesinvoice.config.ViesProperties;
import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.helper.ValidatorHelper;
import com.example.viesinvoice.helper.XsdValidationInterceptor;
import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.model.VatIdentifier;
import com.example.viesinvoice.util.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.ws.test.client.RequestMatchers.payload;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

class ViesSoapContractTest {

    private MockWebServiceServer server;
    private ViesClientServiceImpl client;

    @BeforeEach
    void setUp() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(GeneralConstants.JAXB_CONTEXT_PATH);
        marshaller.afterPropertiesSet();

        WebServiceTemplate template = new WebServiceTemplate(marshaller);
        template.setDefaultUri("http://localhost/vies");
        template.setInterceptors(new XsdValidationInterceptor[]{
                new XsdValidationInterceptor(new ValidatorHelper())
        });
        server = MockWebServiceServer.createServer(template);

        ViesProperties properties = new ViesProperties();
        properties.setEndpoint("http://localhost/vies");
        properties.setMaxAttempts(2);
        properties.setRetryDelay(Duration.ZERO);
        client = new ViesClientServiceImpl(template, properties);
    }

    @Test
    void marshalsSchemaValidRequestAndValidatesResponseBeforeUnmarshalling() {
        server.expect(payload(new StringSource("""
                        <checkVat xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                          <countryCode>DE</countryCode>
                          <vatNumber>129273398</vatNumber>
                        </checkVat>
                        """)))
                .andRespond(withPayload(new StringSource("""
                        <checkVatResponse xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                          <countryCode>DE</countryCode>
                          <vatNumber>129273398</vatNumber>
                          <requestDate>2026-07-18</requestDate>
                          <valid>true</valid>
                          <name>Example GmbH</name>
                          <address>Berlin</address>
                        </checkVatResponse>
                        """)));

        PartyValidationResult result = client.validate(new VatIdentifier("DE", "129273398"));

        assertThat(result.isValid()).isTrue();
        assertThat(result.name()).isEqualTo("Example GmbH");
        server.verify();
    }

    @Test
    void rejectsSchemaInvalidResponse() {
        server.expect(payload(new StringSource("""
                        <checkVat xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                          <countryCode>DE</countryCode>
                          <vatNumber>129273398</vatNumber>
                        </checkVat>
                        """)))
                .andRespond(withPayload(new StringSource("""
                        <checkVatResponse xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
                          <countryCode>DE</countryCode>
                          <vatNumber>129273398</vatNumber>
                          <valid>true</valid>
                        </checkVatResponse>
                        """)));

        assertThatThrownBy(() -> client.validate(new VatIdentifier("DE", "129273398")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(502);
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.RESPONSE_SCHEMA_ERROR_CODE);
                });
        server.verify();
    }
}
