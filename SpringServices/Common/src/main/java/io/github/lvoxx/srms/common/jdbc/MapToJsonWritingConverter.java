package io.github.lvoxx.srms.common.jdbc;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.r2dbc.postgresql.codec.Json;

@WritingConverter
public class MapToJsonWritingConverter implements Converter<Map<String, Object>, Json> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Json convert(@NonNull Map<String, Object> source) {
        try {
            return Json.of(mapper.writeValueAsString(source));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert Map to Json", e);
        }
    }
}
