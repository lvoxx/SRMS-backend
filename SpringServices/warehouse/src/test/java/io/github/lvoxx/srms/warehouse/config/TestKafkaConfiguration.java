package io.github.lvoxx.srms.warehouse.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.github.lvoxx.srms.kafka.warehouse.WarehouseAlertMessage;

/**
 * Test configuration for Kafka consumers used in integration tests.
 */
@TestConfiguration
public class TestKafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    /**
     * Consumer factory for test message verification.
     */
    @Bean
    public ConsumerFactory<String, WarehouseAlertMessage> testConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Schema Registry Configuration
        config.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        config.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Kafka listener container factory for test consumers.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WarehouseAlertMessage> 
            testKafkaListenerContainerFactory(
                ConsumerFactory<String, WarehouseAlertMessage> testConsumerFactory) {
        
        ConcurrentKafkaListenerContainerFactory<String, WarehouseAlertMessage> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(testConsumerFactory);
        
        return factory;
    }
}