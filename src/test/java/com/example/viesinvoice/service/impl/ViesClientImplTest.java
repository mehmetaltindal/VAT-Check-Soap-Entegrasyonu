package com.example.viesinvoice.service.impl;

import com.example.viesinvoice.config.ViesProperties;
import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.model.ValidationStatus;
import com.example.viesinvoice.model.VatIdentifier;
import com.example.viesinvoice.soap.generated.CheckVatResponse;
import com.example.viesinvoice.soap.generated.ObjectFactory;
import com.example.viesinvoice.util.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import javax.xml.datatype.DatatypeFactory;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViesClientImplTest {

    private final VatIdentifier identifier = new VatIdentifier("DE", "129273398");

    private WebServiceTemplate webServiceTemplate;
    private ViesClientServiceImpl client;

    @BeforeEach
    void setUp() {
        webServiceTemplate = mock(WebServiceTemplate.class);
        ViesProperties properties = new ViesProperties();
        properties.setEndpoint("http://localhost/vies");
        properties.setMaxAttempts(2);
        properties.setRetryDelay(Duration.ZERO);
        client = new ViesClientServiceImpl(webServiceTemplate, properties);
    }

    @Test
    void mapsSuccessfulSoapResponse() throws Exception {
        when(send()).thenReturn(response(true));

        PartyValidationResult result = client.validate(identifier);

        assertThat(result.status()).isEqualTo(ValidationStatus.VALID);
        assertThat(result.requestDate()).isEqualTo("2026-07-18");
        assertThat(result.name()).isEqualTo("Example GmbH");
        assertThat(result.address()).isEqualTo("Berlin");
    }

    @Test
    void treatsValidFalseAsBusinessResultWithoutRetry() throws Exception {
        when(send()).thenReturn(response(false));

        PartyValidationResult result = client.validate(identifier);

        assertThat(result.status()).isEqualTo(ValidationStatus.INVALID);
        verify(webServiceTemplate, times(1))
                .marshalSendAndReceive(any(Object.class), any(WebServiceMessageCallback.class));
    }

    @Test
    void doesNotRetryInvalidInputFault() {
        SoapFaultClientException fault = fault("INVALID_INPUT");
        when(send()).thenThrow(fault);

        assertThatThrownBy(() -> client.validate(identifier))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(422);
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.INVALID_INPUT_CODE);
                });
        verify(webServiceTemplate, times(1))
                .marshalSendAndReceive(any(Object.class), any(WebServiceMessageCallback.class));
    }

    @Test
    void retriesTransientFaultAndThenReturnsResponse() throws Exception {
        SoapFaultClientException fault = fault("MS_UNAVAILABLE");
        when(send())
                .thenThrow(fault)
                .thenReturn(response(true));

        PartyValidationResult result = client.validate(identifier);

        assertThat(result.status()).isEqualTo(ValidationStatus.VALID);
        verify(webServiceTemplate, times(2))
                .marshalSendAndReceive(any(Object.class), any(WebServiceMessageCallback.class));
    }

    @Test
    void mapsExhaustedTransientFaultToServiceUnavailable() {
        SoapFaultClientException fault = fault("MS_MAX_CONCURRENT_REQ");
        when(send()).thenThrow(fault);

        assertThatThrownBy(() -> client.validate(identifier))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(503);
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.SERVICE_UNAVAILABLE_CODE);
                });
        verify(webServiceTemplate, times(2))
                .marshalSendAndReceive(any(Object.class), any(WebServiceMessageCallback.class));
    }

    @Test
    void mapsExhaustedSocketTimeoutToGatewayTimeout() {
        WebServiceIOException timeout =
                new WebServiceIOException("timeout", new SocketTimeoutException("read timed out"));
        when(send()).thenThrow(timeout);

        assertThatThrownBy(() -> client.validate(identifier))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(504);
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.TIMEOUT_CODE);
                });
        verify(webServiceTemplate, times(2))
                .marshalSendAndReceive(any(Object.class), any(WebServiceMessageCallback.class));
    }

    @Test
    void mapsUnknownSoapFaultToBadGatewayWithoutRetry() {
        SoapFaultClientException fault = fault("UNEXPECTED_FAULT");
        when(send()).thenThrow(fault);

        assertThatThrownBy(() -> client.validate(identifier))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(502);
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.UPSTREAM_ERROR_CODE);
                });
        verify(webServiceTemplate, times(1))
                .marshalSendAndReceive(any(Object.class), any(WebServiceMessageCallback.class));
    }

    private Object send() {
        return webServiceTemplate.marshalSendAndReceive(
                any(Object.class),
                any(WebServiceMessageCallback.class)
        );
    }

    private SoapFaultClientException fault(String reason) {
        SoapFaultClientException exception = mock(SoapFaultClientException.class);
        when(exception.getFaultStringOrReason()).thenReturn(reason);
        return exception;
    }

    private CheckVatResponse response(boolean valid) throws Exception {
        ObjectFactory factory = new ObjectFactory();
        CheckVatResponse response = factory.createCheckVatResponse();
        response.setCountryCode("DE");
        response.setVatNumber("129273398");
        response.setRequestDate(DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-07-18"));
        response.setValid(valid);
        response.setName(factory.createCheckVatResponseName("Example GmbH"));
        response.setAddress(factory.createCheckVatResponseAddress("Berlin"));
        return response;
    }
}
