package io.github.lvoxx.srms.kitchen.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.github.lvoxx.srms.kitchen.enums.KitchenOrderStatus;
import io.github.lvoxx.srms.kitchen.enums.OrderItemStatus;
import io.github.lvoxx.srms.kitchen.models.KitchenOrder;
import io.github.lvoxx.srms.kitchen.models.KitchenOrderItem;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@Slf4j
@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("KitchenOrderItem Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KitchenOrderItemRepositoryTest extends AbstractDatabaseTestContainer {

    @Autowired
    private KitchenOrderItemRepository itemRepository;

    @Autowired
    private KitchenOrderRepository orderRepository;

    private KitchenOrder testOrder;
    private KitchenOrderItem item1;
    private KitchenOrderItem item2;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll().block();
        orderRepository.deleteAll().block();

        // Create test order
        testOrder = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .tableLocation("Bàn 5")
                .staffName("Nguyễn Văn A")
                .orderTime(OffsetDateTime.now())
                .status(KitchenOrderStatus.RECEIVED.getValue())
                .build();
        orderRepository.save(testOrder).block();

        // Create test items
        item1 = KitchenOrderItem.builder()
                .id(UUID.randomUUID())
                .kitchenOrderId(testOrder.getId())
                .menuId(UUID.randomUUID())
                .menuName("Gà nướng")
                .quantity(2)
                .unit("phần")
                .itemStatus(OrderItemStatus.PENDING.getValue())
                .build();

        item2 = KitchenOrderItem.builder()
                .id(UUID.randomUUID())
                .kitchenOrderId(testOrder.getId())
                .menuId(UUID.randomUUID())
                .menuName("Cơm trắng")
                .quantity(2)
                .unit("tô")
                .itemStatus(OrderItemStatus.PENDING.getValue())
                .build();

        itemRepository.save(item1).block();
        itemRepository.save(item2).block();
    }

    @Test
    void testSaveAndFindById() {
        KitchenOrderItem newItem = KitchenOrderItem.builder()
                .id(UUID.randomUUID())
                .kitchenOrderId(testOrder.getId())
                .menuId(UUID.randomUUID())
                .menuName("Nước ngọt")
                .quantity(3)
                .unit("lon")
                .itemStatus(OrderItemStatus.READY.getValue())
                .build();

        StepVerifier.create(itemRepository.save(newItem))
                .assertNext(saved -> {
                    assertThat(saved.getMenuName()).isEqualTo("Nước ngọt");
                    assertThat(saved.getQuantity()).isEqualTo(3);
                    assertThat(saved.getUnit()).isEqualTo("lon");
                })
                .verifyComplete();

        StepVerifier.create(itemRepository.findById(newItem.getId()))
                .assertNext(found -> {
                    assertThat(found.getMenuName()).isEqualTo("Nước ngọt");
                })
                .verifyComplete();
    }

    @Test
    void testFindByKitchenOrderId() {
        StepVerifier.create(itemRepository.findByKitchenOrderId(testOrder.getId()))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void testFindByMenuId() {
        UUID menuId = item1.getMenuId();

        StepVerifier.create(itemRepository.findByMenuId(menuId))
                .assertNext(item -> {
                    assertThat(item.getMenuId()).isEqualTo(menuId);
                    assertThat(item.getMenuName()).isEqualTo("Gà nướng");
                })
                .verifyComplete();
    }

    @Test
    void testFindItemsByOrderId() {
        StepVerifier.create(itemRepository.findItemsByOrderId(testOrder.getId()))
                .assertNext(item -> {
                    assertThat(item.getKitchenOrderId()).isEqualTo(testOrder.getId());
                })
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testCountByOrderId() {
        StepVerifier.create(itemRepository.countByOrderId(testOrder.getId()))
                .assertNext(count -> {
                    assertThat(count).isEqualTo(2L);
                })
                .verifyComplete();
    }

    @Test
    void testUpdateItemStatus() {
        item1.setItemStatus(OrderItemStatus.PREPARING.getValue());

        StepVerifier.create(itemRepository.save(item1))
                .assertNext(updated -> {
                    assertThat(updated.getItemStatus()).isEqualTo(OrderItemStatus.PREPARING.getValue());
                })
                .verifyComplete();
    }

    @Test
    void testItemWithSpecialRequest() {
        KitchenOrderItem specialItem = KitchenOrderItem.builder()
                .id(UUID.randomUUID())
                .kitchenOrderId(testOrder.getId())
                .menuId(UUID.randomUUID())
                .menuName("Phở bò")
                .quantity(1)
                .unit("tô")
                .specialRequest("Ít hành, nhiều thịt")
                .itemStatus(OrderItemStatus.PENDING.getValue())
                .build();

        StepVerifier.create(itemRepository.save(specialItem))
                .assertNext(saved -> {
                    assertThat(saved.getSpecialRequest()).isEqualTo("Ít hành, nhiều thịt");
                })
                .verifyComplete();
    }

    @Test
    void testDeleteByKitchenOrderId() {
        StepVerifier.create(itemRepository.deleteByKitchenOrderId(testOrder.getId()))
                .verifyComplete();

        StepVerifier.create(itemRepository.countByOrderId(testOrder.getId()))
                .assertNext(count -> {
                    assertThat(count).isEqualTo(0L);
                })
                .verifyComplete();
    }

    @Test
    void testDeleteSingleItem() {
        UUID itemId = item1.getId();

        StepVerifier.create(itemRepository.deleteById(itemId))
                .verifyComplete();

        StepVerifier.create(itemRepository.findById(itemId))
                .verifyComplete();

        StepVerifier.create(itemRepository.countByOrderId(testOrder.getId()))
                .assertNext(count -> {
                    assertThat(count).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void testCascadeDeleteWhenOrderDeleted() {
        UUID orderId = testOrder.getId();

        StepVerifier.create(orderRepository.deleteById(orderId))
                .verifyComplete();

        // Items should be deleted due to CASCADE
        StepVerifier.create(itemRepository.findByKitchenOrderId(orderId))
                .verifyComplete();
    }
}