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
        OffsetDateTime now = OffsetDateTime.now();

        @BeforeEach
        void setUp() {
                repository.deleteAll().block();

                // Warehouse receive
                warehouseReceive = KitchenInsideHistory.builder()
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
                                .transactionType(TransactionType.DAMAGE_DISPOSAL.getValue())
                                .itemName("Rau xanh")
                                .itemCategory("Rau củ quả")
                                .quantity(new BigDecimal("2"))
                                .unit("kg")
                                .staffName("Lê Văn C")
                                .notes("Rau héo, không còn tươi")
                                .transactionDate(now)
                                .build();

                KitchenInsideHistory externalPurchase2 = KitchenInsideHistory.builder()
                                .transactionType(TransactionType.EXTERNAL_PURCHASE.getValue())
                                .itemName("Dầu ăn")
                                .itemCategory("Gia vị")
                                .quantity(new BigDecimal("5"))
                                .unit("lít")
                                .staffName("Nguyễn Văn D")
                                .transactionDate(now.minusDays(3))
                                .costAmount(new BigDecimal("250000"))
                                .notes("Mua dầu ăn Neptune")
                                .build();

                KitchenInsideHistory externalPurchase3 = KitchenInsideHistory.builder()
                                .transactionType(TransactionType.EXTERNAL_PURCHASE.getValue())
                                .itemName("Khăn lau")
                                .itemCategory("Vật tư tiêu hao")
                                .quantity(new BigDecimal("20"))
                                .unit("cái")
                                .staffName("Trần Thị E")
                                .transactionDate(now.minusDays(4))
                                .costAmount(new BigDecimal("150000"))
                                .notes("Khăn lau bếp")
                                .build();

                KitchenInsideHistory externalPurchase4 = KitchenInsideHistory.builder()
                                .transactionType(TransactionType.EXTERNAL_PURCHASE.getValue())
                                .itemName("Nước mắm")
                                .itemCategory("Gia vị")
                                .quantity(new BigDecimal("3"))
                                .unit("chai")
                                .staffName("Lê Văn F")
                                .transactionDate(now.minusDays(5))
                                .costAmount(new BigDecimal("180000"))
                                .build();

                warehouseReceive = repository.save(warehouseReceive).block();
                externalPurchase = repository.save(externalPurchase).block();
                damageDisposal = repository.save(damageDisposal).block();
                repository.save(externalPurchase2).block();
                repository.save(externalPurchase3).block();
                repository.save(externalPurchase4).block();

                log.debug("Warehouse Id: {}", warehouseReceive.getId());
                log.debug("External Id: {}", externalPurchase.getId());
                log.info("Damaged Id: {}", damageDisposal.getId());
        }

        @Test
        @DisplayName("Debug: Print all saved rows from setUp")
        void debugPrintAllRows() {
                log.info("=".repeat(80));
                log.info("DEBUG: Printing all rows saved in setUp()");
                log.info("=".repeat(80));

                StepVerifier
                                .create(repository.findAll())
                                .thenConsumeWhile(record -> {
                                        log.info("""

                                                        ========================================
                                                        ID:                {}
                                                        Transaction Type:  {}
                                                        Item Name:         {}
                                                        Item Category:     {}
                                                        Quantity:          {} {}
                                                        Cost Amount:       {}
                                                        Staff Name:        {}
                                                        Transaction Date:  {}
                                                        Source Reference:  {}
                                                        Notes:             {}
                                                        Created At:        {}
                                                        Updated At:        {}
                                                        ========================================
                                                        """,
                                                        record.getId(),
                                                        record.getTransactionType(),
                                                        record.getItemName(),
                                                        record.getItemCategory(),
                                                        record.getQuantity(),
                                                        record.getUnit(),
                                                        record.getCostAmount(),
                                                        record.getStaffName(),
                                                        record.getTransactionDate(),
                                                        record.getSourceReference(),
                                                        record.getNotes(),
                                                        record.getCreatedAt(),
                                                        record.getUpdatedAt());
                                        return true;
                                })
                                .verifyComplete();

                log.info("=".repeat(80));
                log.info("DEBUG: Finished printing all rows");
                log.info("=".repeat(80));
        }

        @Test
        void testSaveAndFindById() {
                KitchenInsideHistory dailyCheck = KitchenInsideHistory.builder()
                                .transactionType(TransactionType.DAILY_CHECK.getValue())
                                .itemName("Gạo")
                                .itemCategory("Thực phẩm khô")
                                .quantity(new BigDecimal("50"))
                                .unit("kg")
                                .staffName("Phạm Văn D")
                                .notes("Kiểm kê cuối ngày")
                                .build();

                StepVerifier.create(repository.save(dailyCheck))
                                .assertNext(saved -> {
                                        assertThat(saved.getItemName()).isEqualTo("Gạo");
                                        assertThat(saved.getTransactionType())
                                                        .isEqualTo(TransactionType.DAILY_CHECK.getValue());
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
                                repository.findByTransactionType(TransactionType.WAREHOUSE_RECEIVE.getValue()))
                                .thenConsumeWhile(history -> {
                                        assertThat(history.getItemName()).isEqualTo("Thịt bò");
                                        assertThat(history.getStaffName())
                                                        .isEqualTo("Nguyễn Văn A");
                                        return true;
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
                OffsetDateTime startDate = now.minusDays(3);
                OffsetDateTime endDate = now.plusDays(1);

                StepVerifier.create(repository.findByDateRange(startDate, endDate))
                                .expectNextCount(4)
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
                OffsetDateTime startDate = now.minusDays(2);

                StepVerifier.create(
                                repository.findByTypeAndDateAfter(
                                                TransactionType.EXTERNAL_PURCHASE.getValue(),
                                                startDate))
                                .assertNext(history -> {
                                        log.info("""

                                                        ========================================
                                                        ID:                {}
                                                        Transaction Type:  {}
                                                        Item Name:         {}
                                                        Item Category:     {}
                                                        Quantity:          {} {}
                                                        Cost Amount:       {}
                                                        Staff Name:        {}
                                                        Transaction Date:  {}
                                                        Source Reference:  {}
                                                        Notes:             {}
                                                        Created At:        {}
                                                        Updated At:        {}
                                                        ========================================
                                                        """,
                                                        history.getId(),
                                                        history.getTransactionType(),
                                                        history.getItemName(),
                                                        history.getItemCategory(),
                                                        history.getQuantity(),
                                                        history.getUnit(),
                                                        history.getCostAmount(),
                                                        history.getStaffName(),
                                                        history.getTransactionDate(),
                                                        history.getSourceReference(),
                                                        history.getNotes(),
                                                        history.getCreatedAt(),
                                                        history.getUpdatedAt());
                                        assertThat(history.getTransactionType())
                                                        .isEqualTo(TransactionType.EXTERNAL_PURCHASE.getValue());
                                })
                                .verifyComplete();
        }

        @Test
        void verifyExternalPurchaseData() {
                StepVerifier
                                .create(repository.findByTransactionType(
                                                TransactionType.EXTERNAL_PURCHASE.getValue()))
                                .thenConsumeWhile(record -> {
                                        log.info("Found record: itemName={}, category={}, cost={}, date={}",
                                                        record.getItemName(),
                                                        record.getItemCategory(),
                                                        record.getCostAmount(),
                                                        record.getTransactionDate());

                                        assertThat(record.getItemCategory()).isNotBlank();
                                        assertThat(record.getCostAmount()).isNotNegative();
                                        assertThat(record.getTransactionDate()).isNotNull();
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void testGetCostSummaryByCategory() {
                OffsetDateTime startDate = now.minusDays(7);
                OffsetDateTime endDate = now.plusDays(1);

                StepVerifier
                                .create(repository.getCostSummaryByCategory(
                                                TransactionType.EXTERNAL_PURCHASE.getValue(),
                                                startDate,
                                                endDate))
                                .thenConsumeWhile(result -> {
                                        log.debug("Result of Cost Summary: Category = {}, Total = {}",
                                                        result.itemCategory(),
                                                        result.totalCost());

                                        assertThat(result.itemCategory()).isNotNull();
                                        assertThat(result.totalCost()).isNotNull();
                                        assertThat(result.totalCost()).isGreaterThan(BigDecimal.ZERO);
                                        return true;
                                })
                                .verifyComplete();
        }

        @Test
        void testDamageDisposalWithNotes() {
                KitchenInsideHistory brokenPlate = KitchenInsideHistory.builder()
                                .transactionType(TransactionType.DAMAGE_DISPOSAL.getValue())
                                .itemName("Dĩa sứ")
                                .itemCategory("Đồ dùng bếp")
                                .quantity(new BigDecimal("5"))
                                .unit("cái")
                                .staffName("Hoàng Thị E")
                                .notes("Vỡ trong quá trình rửa")
                                .build();

                StepVerifier.create(repository.save(brokenPlate))
                                .assertNext(saved -> {
                                        assertThat(saved.getNotes()).isNotBlank();
                                        assertThat(saved.getNotes()).containsIgnoringCase("vỡ");
                                })
                                .verifyComplete();
        }

        @Test
        void testTransferOperations() {
                KitchenInsideHistory transferOut = KitchenInsideHistory.builder()
                                .transactionType(TransactionType.TRANSFER_OUT.getValue())
                                .itemName("Nước mắm")
                                .itemCategory("Gia vị")
                                .quantity(new BigDecimal("2"))
                                .unit("chai")
                                .sourceReference("BAR-001")
                                .staffName("Đỗ Văn F")
                                .notes("Chuyển sang khu bar")
                                .build();

                StepVerifier.create(repository.save(transferOut))
                                .assertNext(saved -> {
                                        assertThat(saved.getTransactionType())
                                                        .isEqualTo(TransactionType.TRANSFER_OUT.getValue());
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
                                        assertThat(updated.getCostAmount())
                                                        .isEqualByComparingTo(new BigDecimal("360000"));
                                })
                                .verifyComplete();
        }
}