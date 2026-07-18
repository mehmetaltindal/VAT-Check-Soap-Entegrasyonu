package com.example.viesinvoice.service.impl;

import com.example.viesinvoice.config.ViesProperties;
import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.helper.ValidatorHelper;
import com.example.viesinvoice.helper.XsdValidationInterceptor;
import com.example.viesinvoice.model.VatIdentifier;
import com.example.viesinvoice.util.ApiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViesHttpTimeoutTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesDelayedHttpResponseAndReturnsGatewayTimeout() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/vies", exchange -> {
            try {
                Thread.sleep(300);
                exchange.sendResponseHeaders(200, 0);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();

        ViesProperties properties = new ViesProperties();
        properties.setEndpoint("http://localhost:" + server.getAddress().getPort() + "/vies");
        properties.setConnectTimeout(Duration.ofMillis(100));
        properties.setReadTimeout(Duration.ofMillis(50));
        properties.setMaxAttempts(2);
        properties.setRetryDelay(Duration.ZERO);

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(GeneralConstants.JAXB_CONTEXT_PATH);
        marshaller.afterPropertiesSet();

        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(properties.getConnectTimeout());
        sender.setReadTimeout(properties.getReadTimeout());

        WebServiceTemplate template = new WebServiceTemplate(marshaller);
        template.setDefaultUri(properties.getEndpoint());
        template.setMessageSender(sender);
        template.setInterceptors(new XsdValidationInterceptor[]{
                new XsdValidationInterceptor(new ValidatorHelper())
        });

        ViesClientServiceImpl client = new ViesClientServiceImpl(template, properties);

        assertThatThrownBy(() -> client.validate(new VatIdentifier("DE", "129273398")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(504);
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorConstants.TIMEOUT_CODE);
                });
    }
}
