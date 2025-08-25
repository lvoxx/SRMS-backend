package io.github.lvoxx.srms.customer.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackages = { "io.github.lvoxx.srms.common" }) // Scan ControllerAdvice từ module Common
public class TestConfig {

}
