package io.github.lvoxx.srms.contactor.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;

import io.github.lvoxx.srms.common.jdbc.JsonToMapReadingConverter;
import io.github.lvoxx.srms.common.jdbc.MapToJsonWritingConverter;

@Configuration
public class R2dbcConfig {

    @Bean
    R2dbcCustomConversions r2dbcCustomConversions() {
        List<Object> converters = List.of(
                new MapToJsonWritingConverter(),
                new JsonToMapReadingConverter()
        // + các converters enum nếu bạn có
        );
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    @Bean
    MappingR2dbcConverter mappingR2dbcConverter(R2dbcCustomConversions conversions) {
        R2dbcMappingContext mappingContext = new R2dbcMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        return new MappingR2dbcConverter(mappingContext, conversions);
    }
}
