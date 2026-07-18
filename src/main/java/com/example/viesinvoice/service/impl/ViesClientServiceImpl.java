package com.example.viesinvoice.service.impl;

import com.example.viesinvoice.config.ViesProperties;
import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.model.PartyValidationResult;
import com.example.viesinvoice.model.VatIdentifier;
import com.example.viesinvoice.service.ViesClientService;
import com.example.viesinvoice.soap.generated.CheckVat;
import com.example.viesinvoice.soap.generated.CheckVatResponse;
import com.example.viesinvoice.util.ApiException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.xml.bind.JAXBElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import java.net.SocketTimeoutException;
import java.util.Locale;

@Service
public class ViesClientServiceImpl implements ViesClientService {

    private static final Logger LOGGER = LogManager.getLogger(ViesClientServiceImpl.class);

    private final WebServiceTemplate webServiceTemplate;
    private final Retry retry;

    public ViesClientServiceImpl(WebServiceTemplate webServiceTemplate, ViesProperties properties) {
        this.webServiceTemplate = webServiceTemplate;
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(properties.getMaxAttempts())
                .waitDuration(properties.getRetryDelay())
                .retryOnException(this::isRetryable)
                .build();
        this.retry = Retry.of(GeneralConstants.VIES_RETRY_NAME, retryConfig);
    }

    @Override
    public PartyValidationResult validate(VatIdentifier vatIdentifier) {
        try {
            return retry.executeSupplier(() -> callVies(vatIdentifier));
        } catch (ApiException exception) {
            throw exception;
        } catch (SoapFaultClientException exception) {
            throw mapSoapFault(exception);
        } catch (WebServiceIOException exception) {
            throw mapConnectionFailure(exception);
        } catch (RuntimeException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    ErrorConstants.UPSTREAM_ERROR_CODE,
                    ErrorConstants.UPSTREAM_ERROR_MESSAGE,
                    exception
            );
        }
    }

    private PartyValidationResult callVies(VatIdentifier vatIdentifier) {
        CheckVat request = new CheckVat();
        request.setCountryCode(vatIdentifier.countryCode());
        request.setVatNumber(vatIdentifier.vatNumber());

        Object rawResponse = webServiceTemplate.marshalSendAndReceive(
                request,
                new SoapActionCallback(GeneralConstants.SOAP_ACTION)
        );
        CheckVatResponse response = (CheckVatResponse) rawResponse;
        String requestDate = response.getRequestDate().toXMLFormat();
        LOGGER.info(
                "VIES yanıtı alındı: countryCode={}, valid={}, requestDate={}",
                vatIdentifier.countryCode(),
                response.isValid(),
                requestDate
        );

        return PartyValidationResult.checked(
                vatIdentifier,
                response.isValid(),
                requestDate,
                valueOf(response.getName()),
                valueOf(response.getAddress())
        );
    }

    private boolean isRetryable(Throwable exception) {
        if (exception instanceof WebServiceIOException) {
            return true;
        }
        if (exception instanceof SoapFaultClientException soapFault) {
            return GeneralConstants.TRANSIENT_VIES_FAULTS.contains(
                    normalizedFault(soapFault.getFaultStringOrReason())
            );
        }
        return false;
    }

    private ApiException mapSoapFault(SoapFaultClientException exception) {
        String fault = normalizedFault(exception.getFaultStringOrReason());
        LOGGER.warn("VIES SOAP Fault döndü: fault={}", fault);

        if (GeneralConstants.INVALID_INPUT_FAULT.equals(fault)) {
            return new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorConstants.INVALID_INPUT_CODE,
                    ErrorConstants.INVALID_INPUT_MESSAGE,
                    exception
            );
        }
        if (GeneralConstants.TRANSIENT_VIES_FAULTS.contains(fault)) {
            return new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorConstants.SERVICE_UNAVAILABLE_CODE,
                    ErrorConstants.SERVICE_UNAVAILABLE_MESSAGE,
                    exception
            );
        }
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                ErrorConstants.UPSTREAM_ERROR_CODE,
                ErrorConstants.UPSTREAM_ERROR_MESSAGE,
                exception
        );
    }

    private ApiException mapConnectionFailure(WebServiceIOException exception) {
        if (causedByTimeout(exception)) {
            return new ApiException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    ErrorConstants.TIMEOUT_CODE,
                    ErrorConstants.TIMEOUT_MESSAGE,
                    exception
            );
        }
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorConstants.SERVICE_UNAVAILABLE_CODE,
                ErrorConstants.SERVICE_UNAVAILABLE_MESSAGE,
                exception
        );
    }

    private String normalizedFault(String faultReason) {
        String normalized = faultReason == null ? "" : faultReason.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains(GeneralConstants.INVALID_INPUT_FAULT)) {
            return GeneralConstants.INVALID_INPUT_FAULT;
        }
        return GeneralConstants.TRANSIENT_VIES_FAULTS.stream()
                .filter(normalized::contains)
                .findFirst()
                .orElse(normalized);
    }

    private String valueOf(JAXBElement<String> element) {
        return element == null ? null : element.getValue();
    }

    private boolean causedByTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
