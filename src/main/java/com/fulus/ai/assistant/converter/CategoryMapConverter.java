package com.fulus.ai.assistant.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Converter
@Slf4j
public class CategoryMapConverter implements AttributeConverter<Map<String, BigDecimal>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, BigDecimal> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting categories map to JSON", e);
            return "{}";
        }
    }

    @Override
    public Map<String, BigDecimal> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, BigDecimal>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to categories map", e);
            return new HashMap<>();
        }
    }
}
