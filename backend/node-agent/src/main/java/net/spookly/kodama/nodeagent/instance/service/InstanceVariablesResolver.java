package net.spookly.kodama.nodeagent.instance.service;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class InstanceVariablesResolver {

    private static final TypeReference<Map<String, String>> VARIABLES_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public InstanceVariablesResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> resolve(Map<String, String> variables, String variablesJson) {
        if (variables != null && variablesJson != null) {
            throw new InstancePrepareValidationException("Provide either variables or variablesJson, not both");
        }
        if (variables != null) {
            return variables;
        }
        if (variablesJson == null || variablesJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(variablesJson, VARIABLES_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (JsonProcessingException ex) {
            throw new InstancePrepareValidationException("variablesJson must be a JSON object with string values", ex);
        }
    }
}
