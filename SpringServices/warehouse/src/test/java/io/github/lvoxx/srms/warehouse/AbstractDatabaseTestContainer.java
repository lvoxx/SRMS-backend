package io.github.lvoxx.srms.warehouse;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractDatabaseTestContainer {
    @SuppressWarnings("resource")
    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4-alpine")
            .withDatabaseName("test")
            .withUsername("root")
            .withPassword("Te3tP4ssW@r$")
            .withInitScript("warehouse_test.sql");

    @DynamicPropertySource
    static void configureR2dbc(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url",
                () -> String.format("r2dbc:postgresql://%s:%d/%s",
                        postgres.getHost(),
                        postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                        postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }
}
