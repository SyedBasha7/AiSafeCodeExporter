package com.inneo.aisafecodesync.core.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

@Component
public class JsonReportWriter {

    private final ObjectMapper objectMapper;

    public JsonReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String write(SyncReport report) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write JSON report.", ex);
        }
    }
}
