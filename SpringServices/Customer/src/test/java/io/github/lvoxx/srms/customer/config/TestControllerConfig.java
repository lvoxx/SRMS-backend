package io.github.lvoxx.srms.customer.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.EnableWebFlux;

import io.github.lvoxx.srms.common.config.MessageConfig;

@TestConfiguration
@EnableWebFlux
@Import({
        MessageConfig.class
})
@ImportAutoConfiguration({
        ValidationAutoConfiguration.class,
        MessageSourceAutoConfiguration.class
})
@ComponentScan(basePackages = { "io.github.lvoxx.srms" }, includeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MessageConfig.class)
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io.github.lvoxx.srms.customer.repositories.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io.github.lvoxx.srms.customer.config.*"),
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {
                org.springframework.stereotype.Service.class,
                org.springframework.cache.annotation.EnableCaching.class,
                org.springframework.context.annotation.Configuration.class
        }),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*CacheManager.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*CacheConfig.*")
})
public class TestControllerConfig {
}
