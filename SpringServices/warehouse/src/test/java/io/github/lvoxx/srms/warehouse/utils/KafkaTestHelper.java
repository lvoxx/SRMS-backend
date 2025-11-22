package io.github.lvoxx.srms.warehouse.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for Kafka integration tests.
 * Provides utilities for setting up test consumers and collecting messages.
 */
@Slf4j
public class KafkaTestHelper<K, V> {

    private final EmbeddedKafkaBroker embeddedKafka;
    private final String topic;
    private final BlockingQueue<ConsumerRecord<K, V>> records;
    private KafkaMessageListenerContainer<K, V> container;

    public KafkaTestHelper(EmbeddedKafkaBroker embeddedKafka, String topic) {
        this.embeddedKafka = embeddedKafka;
        this.topic = topic;
        this.records = new LinkedBlockingQueue<>();
    }

    /**
     * Sets up a test consumer for Avro messages.
     */
    public void setupAvroConsumer(String groupId) {
        Map<String, Object> consumerProps = new HashMap<>(
            KafkaTestUtils.consumerProps(groupId, "true", embeddedKafka));
        
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://test-registry");
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        consumerProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        consumerProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        DefaultKafkaConsumerFactory<K, V> consumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setGroupId(groupId);
        
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        
        container.setupMessageListener((MessageListener<K, V>) record -> {
            log.debug("Test consumer received message: topic={}, partition={}, offset={}, key={}", 
                     record.topic(), record.partition(), record.offset(), record.key());
            records.add(record);
        });
        
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
        
        log.info("Test consumer container started for topic: {}", topic);
    }

    /**
     * Polls for a single message with timeout.
     */
    public ConsumerRecord<K, V> pollOne(long timeout, TimeUnit unit) throws InterruptedException {
        ConsumerRecord<K, V> record = records.poll(timeout, unit);
        if (record == null) {
            log.warn("No message received within {} {}", timeout, unit);
        }
        return record;
    }

    /**
     * Polls for multiple messages with timeout.
     */
    public List<ConsumerRecord<K, V>> pollMany(int count, long timeoutPerMessage, TimeUnit unit) 
            throws InterruptedException {
        List<ConsumerRecord<K, V>> result = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            ConsumerRecord<K, V> record = records.poll(timeoutPerMessage, unit);
            if (record == null) {
                log.warn("Expected {} messages but received only {}", count, result.size());
                break;
            }
            result.add(record);
        }
        
        return result;
    }

    /**
     * Drains all available messages without waiting.
     */
    public List<ConsumerRecord<K, V>> drainAll() {
        List<ConsumerRecord<K, V>> result = new ArrayList<>();
        records.drainTo(result);
        return result;
    }

    /**
     * Gets the current queue size.
     */
    public int getQueueSize() {
        return records.size();
    }

    /**
     * Clears all received messages.
     */
    public void clear() {
        records.clear();
    }

    /**
     * Stops the test consumer.
     */
    public void stop() {
        if (container != null) {
            container.stop();
            log.info("Test consumer container stopped for topic: {}", topic);
        }
        records.clear();
    }

    /**
     * Waits for at least N messages to arrive.
     */
    public boolean waitForMessages(int expectedCount, long timeout, TimeUnit unit) 
            throws InterruptedException {
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
        
        while (System.currentTimeMillis() < endTime) {
            if (records.size() >= expectedCount) {
                log.info("Received {} messages (expected {})", records.size(), expectedCount);
                return true;
            }
            Thread.sleep(100);
        }
        
        log.warn("Timeout waiting for {} messages, received only {}", expectedCount, records.size());
        return false;
    }
}