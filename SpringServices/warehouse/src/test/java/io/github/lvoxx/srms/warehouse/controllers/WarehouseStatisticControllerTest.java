package io.github.lvoxx.srms.warehouse.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import io.github.lvoxx.srms.controllerhandler.controller.GlobalExceptionHandler;
import io.github.lvoxx.srms.controllerhandler.controller.ValidationExceptionHandler;
import io.github.lvoxx.srms.warehouse.config.TestControllerWithMessagesConfig;

@DisplayName("Warehouse Statistic Controller Validation Tests")
@Tags({
        @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@WebFluxTest(controllers = WarehouseStatisticController.class)
@Import(TestControllerWithMessagesConfig.class)
@ContextConfiguration(classes = {
        GlobalExceptionHandler.class,
        ValidationExceptionHandler.class
})
@ActiveProfiles("test")
public class WarehouseStatisticControllerTest {
}
