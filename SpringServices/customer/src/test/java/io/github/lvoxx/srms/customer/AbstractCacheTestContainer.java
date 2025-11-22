package io.github.lvoxx.srms.customer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.testcontainers.RedisContainer;

import io.github.lvoxx.srms.customer.config.NoCacheLoadConfig;

@Testcontainers
@Import(NoCacheLoadConfig.class)
public abstract class AbstractCacheTestContainer {

    @SuppressWarnings("resource")
    @Container
    protected static RedisContainer redis = new RedisContainer("redis:7.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureCache(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
    }

    @BeforeAll
    static void setup() {
        redis.start();
    }

    @AfterAll
    static void tearOut() {
        redis.stop();
    }
}
