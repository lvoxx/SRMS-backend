package io.github.lvoxx.srms.warehouse.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.warehouse.helper.MinimalWebFluxTest;

@DisplayName("Warehouse Statistic Controller Validation Tests")
@Tags({
                @Tag("Controller"), @Tag("Validation"), @Tag("Mock")
})
@MinimalWebFluxTest(controllers = WarehouseStatisticController.class, controllersClasses = WarehouseStatisticController.class)
@ActiveProfiles("test")
public class WarehouseStatisticControllerTest {
}
