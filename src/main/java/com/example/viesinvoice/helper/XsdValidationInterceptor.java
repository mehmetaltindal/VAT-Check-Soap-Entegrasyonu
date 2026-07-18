package com.example.viesinvoice.helper;

import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

public class XsdValidationInterceptor implements ClientInterceptor {

    private final ValidatorHelper validatorHelper;

    public XsdValidationInterceptor(ValidatorHelper validatorHelper) {
        this.validatorHelper = validatorHelper;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext) {
        validatorHelper.validateRequest(messageContext.getRequest().getPayloadSource());
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) {
        validatorHelper.validateResponse(messageContext.getResponse().getPayloadSource());
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext) {
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception exception) {
        // No per-request resources are retained.
    }
}
