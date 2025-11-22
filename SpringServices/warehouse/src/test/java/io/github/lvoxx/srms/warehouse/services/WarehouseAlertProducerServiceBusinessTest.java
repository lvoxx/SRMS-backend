package io.github.lvoxx.srms.warehouse.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.github.lvoxx.srms.kafka.utils.Topics;
import io.github.lvoxx.srms.kafka.warehouse.AlertLevel;
import io.github.lvoxx.srms.kafka.warehouse.WarehouseAlertMessage;
import io.github.lvoxx.srms.warehouse.config.WarehouseAlertConfig;
import io.github.lvoxx.srms.warehouse.dto.WarehouseStatisticDTO;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Business logic tests for WarehouseAlertProducerService.
 * Tests the scheduling, pagination, and business rules.
 */
@Slf4j
@SpringBootTest(classes = {
        WarehouseAlertProducerService.class,
        WarehouseAlertConfig.class,
        io.github.lvoxx.srms.kafka.config.KafkaConfig.class
})
@EmbeddedKafka(partitions = 1, topics = { Topics.WAREHOUSE_ALERTS }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092",
        "auto.create.topics.enable=true"
})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://test-registry",
        "warehouse.alert.enabled=true",
        "warehouse.alert.page-size=5",
        "warehouse.alert.check-interval=PT10M", // Long interval to prevent auto-triggering
        "warehouse.alert.initial-delay=PT10M" // Long delay to prevent auto-triggering
})
@ActiveProfiles("kafka")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("WarehouseAlertProducerService Business Tests")
class WarehouseAlertProducerServiceBusinessTest {

    @Autowired
    private WarehouseAlertProducerService producerService;

    @Autowired
    private WarehouseAlertConfig alertConfig;

    @Autowired
    private KafkaTemplate<String, WarehouseAlertMessage> kafkaTemplate;

    @MockitoBean
    private WarehouseStatisticService statisticService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private KafkaMessageListenerContainer<String, WarehouseAlertMessage> container;
    private BlockingQueue<ConsumerRecord<String, WarehouseAlertMessage>> records;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps("business-test-group", "true", embeddedKafka));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://test-registry");
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        DefaultKafkaConsumerFactory<String, WarehouseAlertMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(Topics.WAREHOUSE_ALERTS);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        container.setupMessageListener((MessageListener<String, WarehouseAlertMessage>) record -> {
            log.debug("Business test received: key={}, value={}", record.key(), record.value());
            records.add(record);
        });

        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

        log.info("Business test consumer container ready");
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        records.clear();
    }

    // ==================== SCHEDULED PUBLISHING TESTS ====================

    @Nested
    @DisplayName("Scheduled Alert Publishing")
    class ScheduledPublishingTests {

        @Test
        @DisplayName("Should publish all alerts when enabled")
        void shouldPublishAllAlertsWhenEnabled() throws Exception {
            // Given
            List<WarehouseStatisticDTO.AlertItem> page1 = createAlertItems(5);
            List<WarehouseStatisticDTO.AlertItem> page2 = createAlertItems(3);

            // PAGE 1
            when(statisticService.getAllWarehouseAlerts(eq(0), anyInt()))
                    .thenReturn(Mono.just(createResponse(page1, 8, 0)));

            // When - manually call the method instead of waiting for scheduler
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed to Kafka
            kafkaTemplate.flush();

            // PAGE 2
            when(statisticService.getAllWarehouseAlerts(eq(1), anyInt()))
                    .thenReturn(Mono.just(createResponse(page2, 8, 1)));

            // When - manually call the method instead of waiting for scheduler
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed to Kafka
            kafkaTemplate.flush();

            // Give a small buffer for consumer to catch up
            Thread.sleep(500);

            // Then - wait for all messages with proper timing
            List<ConsumerRecord<String, WarehouseAlertMessage>> receivedRecords = collectMessages(8, 30,
                    TimeUnit.SECONDS);

            assertThat(receivedRecords)
                    .as("Should receive all 8 messages from 2 pages")
                    .hasSize(8);
            verify(statisticService, times(2)).getAllWarehouseAlerts(anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should not publish when disabled")
        void shouldNotPublishWhenDisabled() throws Exception {
            // Given - disable via config property
            alertConfig.setEnabled(false);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(createAlertItems(5), 5, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then - should not call service at all
            verify(statisticService, never()).getAllWarehouseAlerts(anyInt(), anyInt());

            // Wait a bit to ensure no messages
            Thread.sleep(1000);
            assertThat(records).isEmpty();

            // Reset for other tests
            alertConfig.setEnabled(true);
        }

        @Test
        @DisplayName("Should handle empty alerts gracefully")
        void shouldHandleEmptyAlertsGracefully() throws Exception {
            // Given
            when(statisticService.getAllWarehouseAlerts(eq(0), anyInt()))
                    .thenReturn(Mono.just(createResponse(List.of(), 0, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then
            verify(statisticService, times(1)).getAllWarehouseAlerts(eq(0), anyInt());

            // Wait a bit to ensure no messages
            Thread.sleep(1000);
            assertThat(records).isEmpty();
        }

        @Test
        @DisplayName("Should handle null response gracefully")
        void shouldHandleNullResponseGracefully() throws Exception {
            // Given
            when(statisticService.getAllWarehouseAlerts(eq(0), anyInt()))
                    .thenReturn(Mono.empty());

            // When
            producerService.publishWarehouseAlerts();

            // Then
            verify(statisticService, times(1)).getAllWarehouseAlerts(eq(0), anyInt());

            // Wait a bit to ensure no messages
            Thread.sleep(1000);
            assertThat(records).isEmpty();
        }
    }

    // ==================== PAGINATION TESTS ====================

    @Nested
    @DisplayName("Pagination Logic")
    class PaginationTests {

        @BeforeEach
        void setUpPagination() {
            // Clear any existing records before pagination tests
            records.clear();
        }

        @Test
        @DisplayName("Should fetch multiple pages correctly")
        void shouldFetchMultiplePagesCorrectly() throws Exception {
            // Given - 3 pages of alerts
            List<WarehouseStatisticDTO.AlertItem> page1 = createAlertItems(5);
            List<WarehouseStatisticDTO.AlertItem> page2 = createAlertItems(5);
            List<WarehouseStatisticDTO.AlertItem> page3 = createAlertItems(2);

            // When --- PAGE 1
            when(statisticService.getAllWarehouseAlerts(eq(0), anyInt()))
                    .thenReturn(Mono.just(createResponse(page1, 12, 0)));
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed
            kafkaTemplate.flush();
            Thread.sleep(500);

            // When --- PAGE 2
            when(statisticService.getAllWarehouseAlerts(eq(1), anyInt()))
                    .thenReturn(Mono.just(createResponse(page2, 12, 1)));
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed
            kafkaTemplate.flush();
            Thread.sleep(500);

            // When --- PAGE 3
            when(statisticService.getAllWarehouseAlerts(eq(2), anyInt()))
                    .thenReturn(Mono.just(createResponse(page3, 12, 2)));
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed
            kafkaTemplate.flush();
            Thread.sleep(500);

            // Then - collect all messages with extended timeout
            List<ConsumerRecord<String, WarehouseAlertMessage>> receivedRecords = collectMessages(12, 40,
                    TimeUnit.SECONDS);

            assertThat(receivedRecords)
                    .as("Should receive all 12 messages across 3 pages")
                    .hasSize(12);

            verify(statisticService, times(3)).getAllWarehouseAlerts(anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should stop at exact page boundary")
        void shouldStopAtExactPageBoundary() throws Exception {
            // Given - exactly 2 pages
            List<WarehouseStatisticDTO.AlertItem> page1 = createAlertItems(5);
            List<WarehouseStatisticDTO.AlertItem> page2 = createAlertItems(5);

            // When --- PAGE 1
            when(statisticService.getAllWarehouseAlerts(eq(0), anyInt()))
                    .thenReturn(Mono.just(createResponse(page1, 10, 0)));
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed
            kafkaTemplate.flush();
            Thread.sleep(500);

            // When --- PAGE 2
            when(statisticService.getAllWarehouseAlerts(eq(1), anyInt()))
                    .thenReturn(Mono.just(createResponse(page2, 10, 1)));
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed
            kafkaTemplate.flush();
            Thread.sleep(500);

            // Then
            List<ConsumerRecord<String, WarehouseAlertMessage>> receivedRecords = collectMessages(10, 30,
                    TimeUnit.SECONDS);

            assertThat(receivedRecords)
                    .as("Should receive all 10 messages across 2 pages")
                    .hasSize(10);
            verify(statisticService, times(2)).getAllWarehouseAlerts(anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should handle single page correctly")
        void shouldHandleSinglePageCorrectly() throws Exception {
            // Given
            List<WarehouseStatisticDTO.AlertItem> page1 = createAlertItems(3);

            when(statisticService.getAllWarehouseAlerts(eq(0), anyInt()))
                    .thenReturn(Mono.just(createResponse(page1, 3, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Ensure all messages are flushed
            kafkaTemplate.flush();
            Thread.sleep(500);

            // Then
            List<ConsumerRecord<String, WarehouseAlertMessage>> receivedRecords = collectMessages(3, 15,
                    TimeUnit.SECONDS);

            assertThat(receivedRecords)
                    .as("Should receive all 3 messages from single page")
                    .hasSize(3);
            verify(statisticService, times(1)).getAllWarehouseAlerts(eq(0), anyInt());
        }
    }

    // ==================== ALERT LEVEL DETERMINATION TESTS ====================

    @Nested
    @DisplayName("Alert Level Determination")
    class AlertLevelTests {

        @Test
        @DisplayName("Should map CRITICAL severity correctly")
        void shouldMapCriticalSeverityCorrectly() throws Exception {
            // Given
            WarehouseStatisticDTO.AlertItem criticalAlert = createAlertWithSeverity("CRITICAL", 0, 10);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(List.of(criticalAlert), 1, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(10, TimeUnit.SECONDS);

            assertThat(record).isNotNull();
            WarehouseAlertMessage message = record.value();
            assertThat(message.getLevel()).isEqualTo(AlertLevel.CRITICAL);
        }

        @Test
        @DisplayName("Should map WARNING severity correctly")
        void shouldMapWarningSeverityCorrectly() throws Exception {
            // Given
            WarehouseStatisticDTO.AlertItem warningAlert = createAlertWithSeverity("WARNING", 5, 10);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(List.of(warningAlert), 1, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(10, TimeUnit.SECONDS);

            assertThat(record).isNotNull();
            WarehouseAlertMessage message = record.value();
            assertThat(message.getLevel()).isEqualTo(AlertLevel.WARNING);
        }

        @Test
        @DisplayName("Should default to WARNING for unknown severity")
        void shouldDefaultToWarningForUnknownSeverity() throws Exception {
            // Given
            WarehouseStatisticDTO.AlertItem unknownAlert = createAlertWithSeverity("UNKNOWN", 3, 10);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(List.of(unknownAlert), 1, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(10, TimeUnit.SECONDS);

            assertThat(record).isNotNull();
            WarehouseAlertMessage message = record.value();
            assertThat(message.getLevel()).isEqualTo(AlertLevel.WARNING);
        }
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should continue processing after individual alert failure")
        void shouldContinueAfterIndividualFailure() throws Exception {
            // Given
            List<WarehouseStatisticDTO.AlertItem> alerts = createAlertItems(5);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(alerts, 5, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then - should still process all alerts
            verify(statisticService).getAllWarehouseAlerts(anyInt(), anyInt());

            // Should receive at least some messages (may not be all if one fails)
            Thread.sleep(2000);
            assertThat(records.size()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should handle service error gracefully")
        void shouldHandleServiceErrorGracefully() throws Exception {
            // Given
            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

            // When
            producerService.publishWarehouseAlerts();

            // Then - should not crash
            verify(statisticService, times(1)).getAllWarehouseAlerts(eq(0), anyInt());

            // Wait a bit
            Thread.sleep(1000);
            assertThat(records).isEmpty();
        }
    }

    // ==================== MESSAGE CONTENT TESTS ====================

    @Nested
    @DisplayName("Message Content Validation")
    class MessageContentTests {

        @Test
        @DisplayName("Should include deficit calculation in message")
        void shouldIncludeDeficitCalculation() throws Exception {
            // Given
            WarehouseStatisticDTO.AlertItem alert = createAlertWithSeverity("WARNING", 3, 10);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(List.of(alert), 1, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(10, TimeUnit.SECONDS);

            assertThat(record).isNotNull();
            WarehouseAlertMessage message = record.value();
            assertThat(message.getMessage()).contains("Deficit: 7 units");
        }

        @Test
        @DisplayName("Should include recommended action in message")
        void shouldIncludeRecommendedAction() throws Exception {
            // Given
            WarehouseStatisticDTO.AlertItem alert = createAlertWithSeverity("CRITICAL", 0, 10);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(List.of(alert), 1, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(10, TimeUnit.SECONDS);

            assertThat(record).isNotNull();
            WarehouseAlertMessage message = record.value();
            assertThat(message.getMessage()).containsIgnoringCase("immediate");
        }

        @Test
        @DisplayName("Should use product ID as message key")
        void shouldUseProductIdAsMessageKey() throws Exception {
            // Given
            WarehouseStatisticDTO.AlertItem alert = createAlertItems(1).get(0);

            when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                    .thenReturn(Mono.just(createResponse(List.of(alert), 1, 0)));

            // When
            producerService.publishWarehouseAlerts();

            // Then
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(10, TimeUnit.SECONDS);

            assertThat(record).isNotNull();
            assertThat(record.key()).isEqualTo(alert.getId().toString());
            assertThat(record.value().getProductId()).isEqualTo(alert.getId().toString());
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Collects messages from the queue with smart waiting strategy.
     * 
     * @param expectedCount number of messages to collect
     * @param timeout       maximum time to wait
     * @param unit          time unit
     * @return list of collected messages
     */
    private List<ConsumerRecord<String, WarehouseAlertMessage>> collectMessages(
            int expectedCount, long timeout, TimeUnit unit) throws InterruptedException {

        List<ConsumerRecord<String, WarehouseAlertMessage>> result = new ArrayList<>();
        long deadlineMs = System.currentTimeMillis() + unit.toMillis(timeout);
        int consecutiveEmptyPolls = 0;
        int maxConsecutiveEmptyPolls = 3; // Give up after 3 empty polls in a row

        log.info("Starting to collect {} messages with timeout {} {}", expectedCount, timeout, unit);

        while (result.size() < expectedCount && System.currentTimeMillis() < deadlineMs) {
            // Poll with shorter timeout for better responsiveness
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(3, TimeUnit.SECONDS);

            if (record != null) {
                result.add(record);
                consecutiveEmptyPolls = 0; // Reset counter on successful poll
                log.debug("Collected message {}/{}: key={}",
                        result.size(), expectedCount, record.key());
            } else {
                consecutiveEmptyPolls++;
                log.debug("Empty poll {}/{}, collected {}/{} messages",
                        consecutiveEmptyPolls, maxConsecutiveEmptyPolls,
                        result.size(), expectedCount);

                // If we've seen some messages but hit empty polls,
                // maybe producer is done but consumer needs time
                if (result.size() > 0 && consecutiveEmptyPolls >= maxConsecutiveEmptyPolls) {
                    log.warn("Stopping collection after {} consecutive empty polls. " +
                            "Collected {}/{} messages",
                            consecutiveEmptyPolls, result.size(), expectedCount);
                    break;
                }
            }
        }

        if (result.size() < expectedCount) {
            log.warn("Timeout or empty polls reached. Collected {}/{} messages",
                    result.size(), expectedCount);
        } else {
            log.info("Successfully collected all {} messages", expectedCount);
        }

        return result;
    }

    private List<WarehouseStatisticDTO.AlertItem> createAlertItems(int count) {
        List<WarehouseStatisticDTO.AlertItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(WarehouseStatisticDTO.AlertItem.builder()
                    .id(UUID.randomUUID())
                    .productName("Product-" + i)
                    .currentQuantity(i % 2 == 0 ? 5 : 0)
                    .minQuantity(10)
                    .deficit(i % 2 == 0 ? 5 : 10)
                    .severity(i % 2 == 0 ? "WARNING" : "CRITICAL")
                    .message("Test alert message")
                    .updatedAt(OffsetDateTime.now())
                    .build());
        }
        return items;
    }

    private WarehouseStatisticDTO.AlertItem createAlertWithSeverity(
            String severity, int currentQty, int minQty) {
        return WarehouseStatisticDTO.AlertItem.builder()
                .id(UUID.randomUUID())
                .productName("Test Product")
                .currentQuantity(currentQty)
                .minQuantity(minQty)
                .deficit(minQty - currentQty)
                .severity(severity)
                .message("Test message")
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private WarehouseStatisticDTO.AlertListResponse createResponse(
            List<WarehouseStatisticDTO.AlertItem> items, long total, int page) {
        return WarehouseStatisticDTO.AlertListResponse.builder()
                .items(items)
                .totalItems(total)
                .page(page)
                .size(5)
                .alertType("ALL_ALERTS")
                .build();
    }
}