package io.github.lvoxx.srms.kitchen.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.kitchen.AbstractDatabaseTestContainer;
import io.github.lvoxx.srms.kitchen.models.KitchenInventoryCurrent;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@Slf4j
@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("KitchenInventoryCurrent Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KitchenInventoryCurrentRepositoryTest extends AbstractDatabaseTestContainer {

    @Autowired
    private KitchenInventoryCurrentRepository repository;

    private KitchenInventoryCurrent normalStock;
    private KitchenInventoryCurrent lowStock;
    private KitchenInventoryCurrent outOfStock;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();

        // Normal stock
        normalStock = KitchenInventoryCurrent.builder()
                .id(UUID.randomUUID())
                .itemName("Gạo")
                .itemCategory("Thực phẩm khô")
                .currentQuantity(new BigDecimal("100"))
                .unit("kg")
                .minThreshold(new BigDecimal("20"))
                .maxThreshold(new BigDecimal("200"))
                .lastUpdated(OffsetDateTime.now())
                .updatedBy("Nguyễn Văn A")
                .build();

        // Low stock
        lowStock = KitchenInventoryCurrent.builder()
                .id(UUID.randomUUID())
                .itemName("Thịt bò")
                .itemCategory("Thực phẩm tươi sống")
                .currentQuantity(new BigDecimal("5"))
                .unit("kg")
                .minThreshold(new BigDecimal("10"))
                .maxThreshold(new BigDecimal("50"))
                .lastUpdated(OffsetDateTime.now())
                .updatedBy("Trần Thị B")
                .build();

        // Out of stock
        outOfStock = KitchenInventoryCurrent.builder()
                .id(UUID.randomUUID())
                .itemName("Bia Heineken")
                .itemCategory("Đồ uống")
                .currentQuantity(BigDecimal.ZERO)
                .unit("lon")
                .minThreshold(new BigDecimal("24"))
                .maxThreshold(new BigDecimal("120"))
                .lastUpdated(OffsetDateTime.now())
                .updatedBy("Lê Văn C")
                .build();

        repository.save(normalStock).block();
        repository.save(lowStock).block();
        repository.save(outOfStock).block();
    }

    @Test
    void testSaveAndFindById() {
        KitchenInventoryCurrent newItem = KitchenInventoryCurrent.builder()
                .id(UUID.randomUUID())
                .itemName("Dầu ăn")
                .itemCategory("Gia vị")
                .currentQuantity(new BigDecimal("15"))
                .unit("lít")
                .minThreshold(new BigDecimal("5"))
                .maxThreshold(new BigDecimal("30"))
                .lastUpdated(OffsetDateTime.now())
                .updatedBy("Phạm Văn D")
                .build();

        StepVerifier.create(repository.save(newItem))
                .assertNext(saved -> {
                    assertThat(saved.getItemName()).isEqualTo("Dầu ăn");
                    assertThat(saved.getCurrentQuantity()).isEqualByComparingTo(new BigDecimal("15"));
                })
                .verifyComplete();

        StepVerifier.create(repository.findById(newItem.getId()))
                .assertNext(found -> {
                    assertThat(found.getItemName()).isEqualTo("Dầu ăn");
                })
                .verifyComplete();
    }

    @Test
    void testFindByItemName() {
        StepVerifier.create(repository.findByItemName("Gạo"))
                .assertNext(item -> {
                    assertThat(item.getItemName()).isEqualTo("Gạo");
                    assertThat(item.getCurrentQuantity()).isEqualByComparingTo(new BigDecimal("100"));
                })
                .verifyComplete();
    }

    @Test
    void testFindByItemCategory() {
        StepVerifier.create(repository.findByItemCategory("Thực phẩm khô"))
                .assertNext(item -> {
                    assertThat(item.getItemCategory()).isEqualTo("Thực phẩm khô");
                    assertThat(item.getItemName()).isEqualTo("Gạo");
                })
                .verifyComplete();
    }

    @Test
    void testFindLowStockItems() {
        StepVerifier.create(repository.findLowStockItems())
                .expectNextCount(2) // lowStock and outOfStock
                .verifyComplete();
    }

    @Test
    void testFindInStockItems() {
        StepVerifier.create(repository.findInStockItems())
                .expectNextCount(2) // normalStock and lowStock (excluding outOfStock)
                .verifyComplete();
    }

    @Test
    void testUpdateQuantity() {
        StepVerifier.create(
                repository.updateQuantity("Gạo", 20.0, "Hoàng Văn E"))
                .verifyComplete();

        StepVerifier.create(repository.findByItemName("Gạo"))
                .assertNext(item -> {
                    assertThat(item.getCurrentQuantity()).isEqualByComparingTo(new BigDecimal("120"));
                })
                .verifyComplete();
    }

    @Test
    void testUpdateInventory() {
        normalStock.setCurrentQuantity(new BigDecimal("150"));
        normalStock.setUpdatedBy("Đỗ Thị F");

        StepVerifier.create(repository.save(normalStock))
                .assertNext(updated -> {
                    assertThat(updated.getCurrentQuantity()).isEqualByComparingTo(new BigDecimal("150"));
                    assertThat(updated.getUpdatedBy()).isEqualTo("Đỗ Thị F");
                })
                .verifyComplete();
    }

    @Test
    void testInventoryWithNoThresholds() {
        KitchenInventoryCurrent noThreshold = KitchenInventoryCurrent.builder()
                .id(UUID.randomUUID())
                .itemName("Khăn lạnh")
                .itemCategory("Vật dụng")
                .currentQuantity(new BigDecimal("50"))
                .unit("cái")
                .minThreshold(null)
                .maxThreshold(null)
                .lastUpdated(OffsetDateTime.now())
                .updatedBy("Bùi Văn G")
                .build();

        StepVerifier.create(repository.save(noThreshold))
                .assertNext(saved -> {
                    assertThat(saved.getMinThreshold()).isNull();
                    assertThat(saved.getMaxThreshold()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void testDeleteInventoryItem() {
        UUID itemId = outOfStock.getId();

        StepVerifier.create(repository.deleteById(itemId))
                .verifyComplete();

        StepVerifier.create(repository.findById(itemId))
                .verifyComplete();
    }

    @Test
    void testDecreaseQuantity() {
        lowStock.setCurrentQuantity(lowStock.getCurrentQuantity().subtract(new BigDecimal("2")));

        StepVerifier.create(repository.save(lowStock))
                .assertNext(updated -> {
                    assertThat(updated.getCurrentQuantity()).isEqualByComparingTo(new BigDecimal("3"));
                })
                .verifyComplete();
    }

    @Test
    void testItemNameUniqueness() {
        KitchenInventoryCurrent duplicate = KitchenInventoryCurrent.builder()
                .id(UUID.randomUUID())
                .itemName("Gạo") // Duplicate name
                .itemCategory("Thực phẩm khô")
                .currentQuantity(new BigDecimal("50"))
                .unit("kg")
                .lastUpdated(OffsetDateTime.now())
                .updatedBy("Test User")
                .build();

        // This should fail due to UNIQUE constraint on item_name
        StepVerifier.create(repository.save(duplicate))
                .expectError()
                .verify();
    }

    @Test
    void testFindAllInventory() {
        StepVerifier.create(repository.findAll())
                .expectNextCount(3)
                .verifyComplete();
    }
}