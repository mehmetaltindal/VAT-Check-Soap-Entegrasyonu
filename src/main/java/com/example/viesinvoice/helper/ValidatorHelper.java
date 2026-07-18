package com.example.viesinvoice.helper;

import com.example.viesinvoice.constants.ErrorConstants;
import com.example.viesinvoice.constants.GeneralConstants;
import com.example.viesinvoice.util.ApiException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;

@Component
public class ValidatorHelper {

    private final Schema schema;

    public ValidatorHelper() {
        this.schema = loadSchema();
    }

    public void validateRequest(Source payload) {
        validate(
                payload,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorConstants.REQUEST_SCHEMA_ERROR_CODE,
                ErrorConstants.REQUEST_SCHEMA_ERROR_MESSAGE
        );
    }

    public void validateResponse(Source payload) {
        validate(
                payload,
                HttpStatus.BAD_GATEWAY,
                ErrorConstants.RESPONSE_SCHEMA_ERROR_CODE,
                ErrorConstants.RESPONSE_SCHEMA_ERROR_MESSAGE
        );
    }

    private void validate(Source payload, HttpStatus status, String code, String message) {
        try {
            schema.newValidator().validate(payload);
        } catch (SAXException | IOException exception) {
            throw new ApiException(status, code, message, exception);
        }
    }

    private Schema loadSchema() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return schemaFactory.newSchema(new ClassPathResource(GeneralConstants.VIES_XSD_RESOURCE).getURL());
        } catch (SAXException | IOException exception) {
            throw new IllegalStateException("Unable to load the bundled VIES XSD", exception);
        }
    }
}
