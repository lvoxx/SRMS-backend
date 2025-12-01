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
import io.github.lvoxx.srms.kitchen.enums.TransactionType;
import io.github.lvoxx.srms.kitchen.models.KitchenInsideHistory;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@Slf4j
@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("KitchenInsideHistory Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class KitchenInsideHistoryRepositoryTest extends AbstractDatabaseTestContainer {
    @Autowired
    private KitchenInsideHistoryRepository repository;

    private KitchenInsideHistory warehouseReceive;
    private KitchenInsideHistory externalPurchase;
    private KitchenInsideHistory damageDisposal;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();

        OffsetDateTime now = OffsetDateTime.now();

        // Warehouse receive
        warehouseReceive = KitchenInsideHistory.builder()
                .id(UUID.randomUUID())
                .transactionType(TransactionType.WAREHOUSE_RECEIVE.getValue())
                .itemName("Thịt bò")
                .itemCategory("Thực phẩm tươi sống")
                .quantity(new BigDecimal("10.5"))
                .unit("kg")
                .sourceReference("WH-2024-001")
                .staffName("Nguyễn Văn A")
                .transactionDate(now.minusDays(2))
                .build();

        // External purchase
        externalPurchase = KitchenInsideHistory.builder()
                .id(UUID.randomUUID())
                .transactionType(TransactionType.EXTERNAL_PURCHASE.getValue())
                .itemName("Bình gas")
                .itemCategory("Vật tư tiêu hao")
                .quantity(new BigDecimal("1"))
                .unit("bình")
                .staffName("Trần Thị B")
                .transactionDate(now.minusDays(1))
                .costAmount(new BigDecimal("350000"))
                .notes("Mua từ cửa hàng Petrolimex")
                .build();

        // Damage disposal
        damageDisposal = KitchenInsideHistory.builder()
                .id(UUID.randomUUID())
                .transactionType(TransactionType.DAMAGE_DISPOSAL.getValue())
                .itemName("Rau xanh")
                .itemCategory("Rau củ quả")
                .quantity(new BigDecimal("2"))
                .unit("kg")
                .staffName("Lê Văn C")
                .transactionDate(now)
                .notes("Rau héo, không còn tươi")
                .build();

        repository.save(warehouseReceive).block();
        repository.save(externalPurchase).block();
        repository.save(damageDisposal).block();
    }

    @Test
    void testSaveAndFindById() {
        KitchenInsideHistory dailyCheck = KitchenInsideHistory.builder()
                .id(UUID.randomUUID())
                .transactionType(TransactionType.DAILY_CHECK.getValue())
                .itemName("Gạo")
                .itemCategory("Thực phẩm khô")
                .quantity(new BigDecimal("50"))
                .unit("kg")
                .staffName("Phạm Văn D")
                .transactionDate(OffsetDateTime.now())
                .notes("Kiểm kê cuối ngày")
                .build();

        StepVerifier.create(repository.save(dailyCheck))
                .assertNext(saved -> {
                    assertThat(saved.getItemName()).isEqualTo("Gạo");
                    assertThat(saved.getTransactionType()).isEqualTo(TransactionType.DAILY_CHECK.getValue());
                    assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("50"));
                })
                .verifyComplete();

        StepVerifier.create(repository.findById(dailyCheck.getId()))
                .assertNext(found -> {
                    assertThat(found.getItemName()).isEqualTo("Gạo");
                })
                .verifyComplete();
    }

    @Test
    void testFindByTransactionType() {
        StepVerifier.create(
                repository.findByTransactionType(TransactionType.EXTERNAL_PURCHASE.getValue()))
                .assertNext(history -> {
                    assertThat(history.getItemName()).isEqualTo("Bình gas");
                    assertThat(history.getCostAmount()).isEqualByComparingTo(new BigDecimal("350000"));
                })
                .verifyComplete();
    }

    @Test
    void testFindByItemName() {
        StepVerifier.create(repository.findByItemName("Thịt bò"))
                .assertNext(history -> {
                    assertThat(history.getItemName()).isEqualTo("Thịt bò");
                    assertThat(history.getQuantity()).isEqualByComparingTo(new BigDecimal("10.5"));
                })
                .verifyComplete();
    }

    @Test
    void testFindByDateRange() {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(3);
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(1);

        StepVerifier.create(repository.findByDateRange(startDate, endDate))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void testFindHistoryByItemName() {
        StepVerifier.create(repository.findHistoryByItemName("Bình gas"))
                .assertNext(history -> {
                    assertThat(history.getItemName()).isEqualTo("Bình gas");
                    assertThat(history.getItemCategory()).isEqualTo("Vật tư tiêu hao");
                })
                .verifyComplete();
    }

    @Test
    void testFindByTypeAndDateAfter() {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(2);

        StepVerifier.create(
                repository.findByTypeAndDateAfter(
                        TransactionType.WAREHOUSE_RECEIVE.getValue(),
                        startDate))
                .assertNext(history -> {
                    assertThat(history.getTransactionType())
                            .isEqualTo(TransactionType.WAREHOUSE_RECEIVE.getValue());
                })
                .verifyComplete();
    }

    @Test
    void testGetCostSummaryByCategory() {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(7);
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(1);

        StepVerifier.create(repository.getCostSummaryByCategory(startDate, endDate))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void testDamageDisposalWithNotes() {
        KitchenInsideHistory brokenPlate = KitchenInsideHistory.builder()
                .id(UUID.randomUUID())
                .transactionType(TransactionType.DAMAGE_DISPOSAL.getValue())
                .itemName("Dĩa sứ")
                .itemCategory("Đồ dùng bếp")
                .quantity(new BigDecimal("5"))
                .unit("cái")
                .staffName("Hoàng Thị E")
                .transactionDate(OffsetDateTime.now())
                .notes("Vỡ trong quá trình rửa")
                .build();

        StepVerifier.create(repository.save(brokenPlate))
                .assertNext(saved -> {
                    assertThat(saved.getNotes()).isNotBlank();
                    assertThat(saved.getNotes()).contains("vỡ");
                })
                .verifyComplete();
    }

    @Test
    void testTransferOperations() {
        KitchenInsideHistory transferOut = KitchenInsideHistory.builder()
                .id(UUID.randomUUID())
                .transactionType(TransactionType.TRANSFER_OUT.getValue())
                .itemName("Nước mắm")
                .itemCategory("Gia vị")
                .quantity(new BigDecimal("2"))
                .unit("chai")
                .sourceReference("BAR-001")
                .staffName("Đỗ Văn F")
                .transactionDate(OffsetDateTime.now())
                .notes("Chuyển sang khu bar")
                .build();

        StepVerifier.create(repository.save(transferOut))
                .assertNext(saved -> {
                    assertThat(saved.getTransactionType()).isEqualTo(TransactionType.TRANSFER_OUT.getValue());
                    assertThat(saved.getSourceReference()).isEqualTo("BAR-001");
                })
                .verifyComplete();
    }

    @Test
    void testDeleteHistory() {
        UUID historyId = warehouseReceive.getId();

        StepVerifier.create(repository.deleteById(historyId))
                .verifyComplete();

        StepVerifier.create(repository.findById(historyId))
                .verifyComplete();
    }

    @Test
    void testUpdateHistory() {
        externalPurchase.setNotes("Cập nhật: Đã thanh toán");
        externalPurchase.setCostAmount(new BigDecimal("360000"));

        StepVerifier.create(repository.save(externalPurchase))
                .assertNext(updated -> {
                    assertThat(updated.getNotes()).contains("Cập nhật");
                    assertThat(updated.getCostAmount()).isEqualByComparingTo(new BigDecimal("360000"));
                })
                .verifyComplete();
    }
}