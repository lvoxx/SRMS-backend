package io.github.lvoxx.srms.warehouse.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.reactive.config.EnableWebFlux;

@TestConfiguration
@EnableWebFlux
@ImportAutoConfiguration({ HypermediaAutoConfiguration.class })
@ComponentScan(basePackages = { "io.github.lvoxx.srms" }, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io.github.lvoxx.srms.warehouse.repositories.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io.github.lvoxx.srms.warehouse.config.*"),
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {
                                org.springframework.stereotype.Service.class,
                                org.springframework.cache.annotation.EnableCaching.class,
                                org.springframework.context.annotation.Configuration.class
                }),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*CacheManager.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*CacheConfig.*")
})
public class TestControllerOnlyEndpointConfig {
}
