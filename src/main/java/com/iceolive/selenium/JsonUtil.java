package com.iceolive.selenium;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * @author wangmianzhe
 */
public class JsonUtil {
    public static <T> Map<String, T> parse(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String, T>> typeRef = new TypeReference<Map<String, T>>() {
        };
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }
}
