package io.github.lvoxx.srms.warehouse.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.Duration;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
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
import reactor.test.StepVerifier;

/**
 * Kafka message production test for WarehouseAlertProducerService.
 * Tests Kafka message production in isolation with embedded Kafka.
 */
@Slf4j
@SpringBootTest(classes = {
        WarehouseAlertProducerService.class,
        WarehouseAlertConfig.class,
        io.github.lvoxx.srms.kafka.config.KafkaConfig.class
})
@EmbeddedKafka(partitions = 1, topics = { Topics.WAREHOUSE_ALERTS }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://test-registry",
        "warehouse.alert.enabled=true",
        "warehouse.alert.page-size=10",
        "warehouse.alert.retry-attempts=1"
})
@ActiveProfiles("kafka")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("WarehouseAlertProducerService Kafka Tests")
class WarehouseAlertProducerServiceKafkaTest {

    @Autowired
    private WarehouseAlertProducerService producerService;

    @Autowired
    private WarehouseAlertConfig alertConfig;

    @MockitoBean
    private WarehouseStatisticService statisticService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private KafkaMessageListenerContainer<String, WarehouseAlertMessage> container;
    private BlockingQueue<ConsumerRecord<String, WarehouseAlertMessage>> records;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();

        // Configure consumer
        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps("test-consumer", "true", embeddedKafka));
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
            log.debug("Received test message: key={}, value={}", record.key(), record.value());
            records.add(record);
        });

        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

        log.info("Test consumer container started and ready");
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        records.clear();
    }

    // ==================== SINGLE MESSAGE TESTS ====================

    @Test
    @DisplayName("Should publish single WARNING alert message successfully")
    void shouldPublishSingleWarningAlert() throws Exception {
        // Given
        WarehouseStatisticDTO.AlertItem alertItem = createWarningAlert();
        
        // When
        producerService.publishAlertMessage(alertItem).get(10, TimeUnit.SECONDS);

        // Then
        ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(15, TimeUnit.SECONDS);

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(alertItem.getId().toString());

        WarehouseAlertMessage message = record.value();
        assertThat(message.getProductId()).isEqualTo(alertItem.getId().toString());
        assertThat(message.getProductName()).isEqualTo(alertItem.getProductName());
        assertThat(message.getCurrentQuantity()).isEqualTo(alertItem.getCurrentQuantity());
        assertThat(message.getThreshold()).isEqualTo(alertItem.getMinQuantity());
        assertThat(message.getLevel()).isEqualTo(AlertLevel.WARNING);
        assertThat(message.getMessage()).contains("WARNING");
        assertThat(message.getMessage()).contains(alertItem.getProductName());
    }

    @Test
    @DisplayName("Should publish single CRITICAL alert message successfully")
    void shouldPublishSingleCriticalAlert() throws Exception {
        // Given
        WarehouseStatisticDTO.AlertItem alertItem = createCriticalAlert();

        // When
        producerService.publishAlertMessage(alertItem).get(10, TimeUnit.SECONDS);

        // Then
        ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(15, TimeUnit.SECONDS);

        assertThat(record).isNotNull();
        WarehouseAlertMessage message = record.value();

        assertThat(message.getCurrentQuantity()).isZero();
        assertThat(message.getLevel()).isEqualTo(AlertLevel.CRITICAL);
        assertThat(message.getMessage()).contains("CRITICAL");
        assertThat(message.getMessage()).containsIgnoringCase("out of stock");
    }

    @Test
    @DisplayName("Should publish alert reactively")
    void shouldPublishAlertReactively() throws Exception {
        // Given
        WarehouseStatisticDTO.AlertItem alertItem = createWarningAlert();

        // When
        Mono<Void> result = producerService.publishAlertMessageReactive(alertItem);

        // Then
        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(15, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
    }

    // ==================== BATCH PUBLISHING TESTS ====================

    @Test
    @DisplayName("Should publish batch of alerts successfully")
    void shouldPublishBatchOfAlerts() throws Exception {
        // Given
        List<WarehouseStatisticDTO.AlertItem> items = List.of(
                createWarningAlert(),
                createCriticalAlert(),
                createWarningAlert());

        WarehouseStatisticDTO.AlertListResponse batch = WarehouseStatisticDTO.AlertListResponse.builder()
                .items(items)
                .totalItems(3L)
                .page(0)
                .size(10)
                .alertType("ALL_ALERTS")
                .build();

        // When
        Mono<Void> result = producerService.publishAlertBatch(batch);

        // Then
        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        // Collect all 3 messages
        List<ConsumerRecord<String, WarehouseAlertMessage>> receivedRecords = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(5, TimeUnit.SECONDS);
            assertThat(record).isNotNull();
            receivedRecords.add(record);
        }

        assertThat(receivedRecords).hasSize(3);
    }

    @Test
    @DisplayName("Should publish empty batch without error")
    void shouldPublishEmptyBatch() {
        // Given
        WarehouseStatisticDTO.AlertListResponse emptyBatch = WarehouseStatisticDTO.AlertListResponse.builder()
                .items(List.of())
                .totalItems(0L)
                .page(0)
                .size(10)
                .alertType("ALL_ALERTS")
                .build();

        // When
        Mono<Void> result = producerService.publishAlertBatch(emptyBatch);

        // Then
        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    // ==================== MESSAGE FORMAT TESTS ====================

    @Test
    @DisplayName("Should include all required fields in alert message")
    void shouldIncludeAllRequiredFields() throws Exception {
        // Given
        WarehouseStatisticDTO.AlertItem alertItem = createWarningAlert();

        // When
        producerService.publishAlertMessage(alertItem).get(10, TimeUnit.SECONDS);

        // Then
        ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(15, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        WarehouseAlertMessage message = record.value();

        assertThat(message.getMessageId()).isNotNull().isNotEmpty();
        assertThat(message.getProductId()).isNotNull().isNotEmpty();
        assertThat(message.getProductName()).isNotNull().isNotEmpty();
        assertThat(message.getCurrentQuantity()).isNotNegative();
        assertThat(message.getThreshold()).isPositive();
        assertThat(message.getLevel()).isNotNull();
        assertThat(message.getMessage()).isNotNull().isNotEmpty();
        assertThat(message.getTimestamp()).isPositive();
    }

    @Test
    @DisplayName("Should format enhanced message correctly for WARNING")
    void shouldFormatWarningMessageCorrectly() throws Exception {
        // Given
        WarehouseStatisticDTO.AlertItem alertItem = createWarningAlert();

        // When
        producerService.publishAlertMessage(alertItem).get(10, TimeUnit.SECONDS);

        // Then
        ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(15, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        WarehouseAlertMessage message = record.value();
        String enhancedMessage = message.getMessage();

        assertThat(enhancedMessage)
                .contains("WARNING")
                .containsIgnoringCase("below minimum")
                .contains("Current: 5")
                .contains("Minimum: 10")
                .contains("Deficit: 5")
                .containsIgnoringCase("restocking");
    }

    @Test
    @DisplayName("Should format enhanced message correctly for CRITICAL")
    void shouldFormatCriticalMessageCorrectly() throws Exception {
        // Given
        WarehouseStatisticDTO.AlertItem alertItem = createCriticalAlert();

        // When
        producerService.publishAlertMessage(alertItem).get(10, TimeUnit.SECONDS);

        // Then
        ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(15, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        WarehouseAlertMessage message = record.value();
        String enhancedMessage = message.getMessage();

        assertThat(enhancedMessage)
                .contains("CRITICAL")
                .containsIgnoringCase("out of stock")
                .contains("Current: 0")
                .containsIgnoringCase("immediate");
    }

    // ==================== MANUAL TRIGGER TESTS ====================

    @Test
    @DisplayName("Should trigger manual alert check successfully")
    void shouldTriggerManualAlertCheck() throws Exception {
        // Given
        List<WarehouseStatisticDTO.AlertItem> items = List.of(
                createWarningAlert(),
                createCriticalAlert());

        WarehouseStatisticDTO.AlertListResponse response = WarehouseStatisticDTO.AlertListResponse.builder()
                .items(items)
                .totalItems(2L)
                .page(0)
                .size(10)
                .alertType("ALL_ALERTS")
                .build();

        when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                .thenReturn(Mono.just(response));

        // When
        Mono<Long> result = producerService.triggerAlertCheck();

        // Then
        StepVerifier.create(result)
                .expectNext(2L)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        // Verify messages were sent
        List<ConsumerRecord<String, WarehouseAlertMessage>> receivedRecords = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ConsumerRecord<String, WarehouseAlertMessage> record = records.poll(5, TimeUnit.SECONDS);
            if (record != null) {
                receivedRecords.add(record);
            }
        }

        assertThat(receivedRecords).hasSize(2);
    }

    @Test
    @DisplayName("Should handle manual trigger with no alerts")
    void shouldHandleManualTriggerWithNoAlerts() {
        // Given
        WarehouseStatisticDTO.AlertListResponse emptyResponse = WarehouseStatisticDTO.AlertListResponse.builder()
                .items(List.of())
                .totalItems(0L)
                .page(0)
                .size(10)
                .alertType("ALL_ALERTS")
                .build();

        when(statisticService.getAllWarehouseAlerts(anyInt(), anyInt()))
                .thenReturn(Mono.just(emptyResponse));

        // When
        Mono<Long> result = producerService.triggerAlertCheck();

        // Then
        StepVerifier.create(result)
                .expectNext(0L)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    // ==================== HELPER METHODS ====================

    private WarehouseStatisticDTO.AlertItem createWarningAlert() {
        return WarehouseStatisticDTO.AlertItem.builder()
                .id(UUID.randomUUID())
                .productName("Test Product - Low Stock")
                .currentQuantity(5)
                .minQuantity(10)
                .deficit(5)
                .severity("WARNING")
                .message("Below minimum by 5 units")
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private WarehouseStatisticDTO.AlertItem createCriticalAlert() {
        return WarehouseStatisticDTO.AlertItem.builder()
                .id(UUID.randomUUID())
                .productName("Test Product - Out of Stock")
                .currentQuantity(0)
                .minQuantity(10)
                .deficit(10)
                .severity("CRITICAL")
                .message("Product is out of stock")
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}