package io.github.lvoxx.srms.common.utils;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            if (List.class.isAssignableFrom(clazz)) {
                return (T) Collections.emptyList();
            }
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to object", e);
            if (List.class.isAssignableFrom(clazz)) {
                return (T) Collections.emptyList();
            }
            return null;
        }
    }

    public static <T> List<T> fromJsonToList(String json, Class<T> elementClass) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            return objectMapper.readValue(json,
                    typeFactory.constructCollectionType(List.class, elementClass));
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to List", e);
            return Collections.emptyList();
        }
    }
}