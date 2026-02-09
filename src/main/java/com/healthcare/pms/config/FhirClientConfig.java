package com.healthcare.pms.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirClientConfig {

    @Value("${fhir.server.base-url}")
    private String fhirServerBaseUrl;

    @Value("${fhir.server.timeout:60000}")
    private int timeout;

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        fhirContext.getRestfulClientFactory().setSocketTimeout(timeout);
        
        IGenericClient client = fhirContext.newRestfulGenericClient(fhirServerBaseUrl);
        
        // Add logging interceptor for debugging
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(true);
        loggingInterceptor.setLogResponseSummary(true);
        client.registerInterceptor(loggingInterceptor);
        
        return client;
    }
}
