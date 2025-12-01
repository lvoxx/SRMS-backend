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
import io.github.lvoxx.srms.kitchen.models.KitchenOrder;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@Slf4j
@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("KitchenOrder Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class KitchenOrderRepositoryTest extends AbstractDatabaseTestContainer {
    @Autowired
    private KitchenOrderRepository repository;

    private KitchenOrder receivedOrder;
    private KitchenOrder preparingOrder;
    private KitchenOrder completedOrder;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();

        OffsetDateTime now = OffsetDateTime.now();

        // Received order
        receivedOrder = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .tableLocation("Bàn 5")
                .staffName("Nguyễn Văn A")
                .orderTime(now.minusMinutes(10))
                .status(KitchenOrderStatus.RECEIVED.getValue())
                .build();

        // Preparing order
        preparingOrder = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .tableLocation("Bàn 3")
                .staffName("Trần Thị B")
                .orderTime(now.minusMinutes(5))
                .status(KitchenOrderStatus.PREPARING.getValue())
                .build();

        // Completed order
        completedOrder = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .tableLocation("Bàn 1")
                .staffName("Lê Văn C")
                .orderTime(now.minusHours(1))
                .status(KitchenOrderStatus.COMPLETED.getValue())
                .completedAt(now.minusMinutes(30))
                .build();

        repository.save(receivedOrder).block();
        repository.save(preparingOrder).block();
        repository.save(completedOrder).block();
    }

    @Test
    void testSaveAndFindById() {
        KitchenOrder newOrder = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .tableLocation("Bàn 10")
                .staffName("Phạm Văn D")
                .orderTime(OffsetDateTime.now())
                .status(KitchenOrderStatus.RECEIVED.getValue())
                .build();

        StepVerifier.create(repository.save(newOrder))
                .assertNext(saved -> {
                    assertThat(saved.getTableLocation()).isEqualTo("Bàn 10");
                    assertThat(saved.getStatus()).isEqualTo(KitchenOrderStatus.RECEIVED.getValue());
                })
                .verifyComplete();

        StepVerifier.create(repository.findById(newOrder.getId()))
                .assertNext(found -> {
                    assertThat(found.getStaffName()).isEqualTo("Phạm Văn D");
                })
                .verifyComplete();
    }

    @Test
    void testFindByOrderId() {
        StepVerifier.create(repository.findByOrderId(receivedOrder.getOrderId()))
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(receivedOrder.getId());
                    assertThat(order.getTableLocation()).isEqualTo("Bàn 5");
                })
                .verifyComplete();
    }

    @Test
    void testFindByStatus() {
        StepVerifier.create(repository.findByStatus(KitchenOrderStatus.PREPARING.getValue()))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(KitchenOrderStatus.PREPARING.getValue());
                    assertThat(order.getTableLocation()).isEqualTo("Bàn 3");
                })
                .verifyComplete();
    }

    @Test
    void testFindByStatusOrderByOrderTimeDesc() {
        StepVerifier.create(
                repository.findByStatusOrderByOrderTimeDesc(KitchenOrderStatus.RECEIVED.getValue()))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(KitchenOrderStatus.RECEIVED.getValue());
                })
                .verifyComplete();
    }

    @Test
    void testFindByDateRange() {
        OffsetDateTime startDate = OffsetDateTime.now().minusHours(2);
        OffsetDateTime endDate = OffsetDateTime.now().plusHours(1);

        StepVerifier.create(repository.findByDateRange(startDate, endDate))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void testFindActiveOrders() {
        StepVerifier.create(repository.findActiveOrders())
                .expectNextCount(2) // received and preparing
                .verifyComplete();
    }

    @Test
    void testCountByStatus() {
        StepVerifier.create(repository.countByStatus(KitchenOrderStatus.COMPLETED.getValue()))
                .assertNext(count -> {
                    assertThat(count).isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void testUpdateOrderStatus() {
        receivedOrder.setStatus(KitchenOrderStatus.PREPARING.getValue());

        StepVerifier.create(repository.save(receivedOrder))
                .assertNext(updated -> {
                    assertThat(updated.getStatus()).isEqualTo(KitchenOrderStatus.PREPARING.getValue());
                })
                .verifyComplete();
    }

    @Test
    void testOrderWithKitchenNote() {
        KitchenOrder rejectedOrder = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .tableLocation("Bàn 7")
                .staffName("Hoàng Văn E")
                .orderTime(OffsetDateTime.now())
                .status(KitchenOrderStatus.KITCHEN_REJECTED.getValue())
                .kitchenNote("Hết nguyên liệu")
                .build();

        StepVerifier.create(repository.save(rejectedOrder))
                .assertNext(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(KitchenOrderStatus.KITCHEN_REJECTED.getValue());
                    assertThat(saved.getKitchenNote()).isEqualTo("Hết nguyên liệu");
                })
                .verifyComplete();
    }

    @Test
    void testOrderWithCustomerNote() {
        KitchenOrder customerRejectedOrder = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .tableLocation("Bàn 8")
                .staffName("Đỗ Thị F")
                .orderTime(OffsetDateTime.now())
                .status(KitchenOrderStatus.CUSTOMER_REJECTED.getValue())
                .customerNote("Khách hàng đổi ý")
                .build();

        StepVerifier.create(repository.save(customerRejectedOrder))
                .assertNext(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(KitchenOrderStatus.CUSTOMER_REJECTED.getValue());
                    assertThat(saved.getCustomerNote()).isEqualTo("Khách hàng đổi ý");
                })
                .verifyComplete();
    }

    @Test
    void testDeleteOrder() {
        UUID orderId = completedOrder.getId();

        StepVerifier.create(repository.deleteById(orderId))
                .verifyComplete();

        StepVerifier.create(repository.findById(orderId))
                .verifyComplete();
    }
}