package com.example.viesinvoice.config;

import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.helper.ValidatorHelper;
import com.example.viesinvoice.helper.XsdValidationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

@Configuration
public class SoapClientConfig {

    @Bean
    Jaxb2Marshaller viesMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(GeneralConstants.JAXB_CONTEXT_PATH);
        return marshaller;
    }

    @Bean
    WebServiceTemplate viesWebServiceTemplate(
            Jaxb2Marshaller viesMarshaller,
            ViesProperties properties,
            ValidatorHelper validatorHelper
    ) {
        HttpUrlConnectionMessageSender messageSender = new HttpUrlConnectionMessageSender();
        messageSender.setConnectionTimeout(properties.getConnectTimeout());
        messageSender.setReadTimeout(properties.getReadTimeout());

        WebServiceTemplate template = new WebServiceTemplate(viesMarshaller);
        template.setDefaultUri(properties.getEndpoint());
        template.setMessageSender(messageSender);
        template.setInterceptors(new XsdValidationInterceptor[]{new XsdValidationInterceptor(validatorHelper)});
        return template;
    }
}
