package io.github.lvoxx.srms.warehouse.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@DisplayName("Contactor Service Tests")
@Tags({
                @Tag("Service"), @Tag("Mock"), @Tag("Message"), @Tag("Integate")
})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WarehouseCountServiceTest {
}
