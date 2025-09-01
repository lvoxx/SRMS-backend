package io.github.lvoxx.srms.common.jdbc;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.r2dbc.postgresql.codec.Json;

@ReadingConverter
public class JsonToMapReadingConverter implements Converter<Json, Map<String, Object>> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Map<String, Object> convert(@NonNull Json source) {
        try {
            return mapper.readValue(source.asString(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert Json to Map", e);
        }
    }
}
