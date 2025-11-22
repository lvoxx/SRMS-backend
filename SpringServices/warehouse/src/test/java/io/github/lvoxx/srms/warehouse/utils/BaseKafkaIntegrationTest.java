package io.github.lvoxx.srms.warehouse.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import io.github.lvoxx.srms.kafka.utils.Topics;
import io.github.lvoxx.srms.kafka.warehouse.WarehouseAlertMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for Kafka integration tests.
 * Provides common setup and teardown logic.
 */
@Slf4j
public abstract class BaseKafkaIntegrationTest {

    @Autowired
    protected EmbeddedKafkaBroker embeddedKafka;

    protected KafkaTestHelper<String, WarehouseAlertMessage> kafkaHelper;

    @BeforeEach
    void setUpKafka() {
        log.info("Setting up Kafka test helper for topic: {}", Topics.WAREHOUSE_ALERTS);
        kafkaHelper = new KafkaTestHelper<>(embeddedKafka, Topics.WAREHOUSE_ALERTS);
        kafkaHelper.setupAvroConsumer(getConsumerGroupId());
        log.info("Kafka test helper ready");
    }

    @AfterEach
    void tearDownKafka() {
        if (kafkaHelper != null) {
            kafkaHelper.stop();
        }
    }

    /**
     * Override this to provide a unique consumer group ID for each test class.
     */
    protected abstract String getConsumerGroupId();
}