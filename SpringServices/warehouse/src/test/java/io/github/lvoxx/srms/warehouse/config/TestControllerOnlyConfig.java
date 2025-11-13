package io.github.lvoxx.srms.warehouse.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import io.github.lvoxx.srms.common.config.MessageConfig;

/**
 * Minimal test configuration for controller tests.
 * Does not scan for components - only imports necessary configurations.
 * Use this when testing individual controllers with @WebFluxTest.
 * 
 * IMPORTANT: This config does NOT include WebSocket or any services.
 * Only includes what's needed for basic controller testing.
 */
@TestConfiguration
@Import({
                MessageConfig.class,
})
@ImportAutoConfiguration({
                ValidationAutoConfiguration.class,
                MessageSourceAutoConfiguration.class
})
public class TestControllerOnlyConfig {
}
