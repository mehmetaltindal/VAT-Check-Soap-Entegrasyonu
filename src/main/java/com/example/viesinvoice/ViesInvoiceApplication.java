package com.example.viesinvoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ViesInvoiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ViesInvoiceApplication.class, args);
    }
}
