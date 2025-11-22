package io.github.lvoxx.srms.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.github.lvoxx.srms.kafka.warehouse.WarehouseAlertMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka configuration for warehouse module.
 * <p>
 * Configures producers for Avro-based message serialization
 * with Confluent Schema Registry integration.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url:http://localhost:8081}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private int retries;

    @Value("${spring.kafka.producer.compression-type:snappy}")
    private String compressionType;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private int batchSize;

    @Value("${spring.kafka.producer.linger-ms:10}")
    private int lingerMs;

    @Value("${spring.kafka.producer.buffer-memory:33554432}")
    private long bufferMemory;

    @Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection:5}")
    private int maxInFlightRequestsPerConnection;

    @Value("${spring.kafka.producer.properties.enable.idempotence:true}")
    private boolean enableIdempotence;

    @Value("${spring.kafka.producer.properties.request.timeout.ms:30000}")
    private int requestTimeoutMs;

    @Value("${spring.kafka.producer.properties.delivery.timeout.ms:120000}")
    private int deliveryTimeoutMs;

    @Value("${spring.kafka.producer.properties.max.block.ms:60000}")
    private int maxBlockMs;

    // ==================== PRODUCER CONFIGURATION ====================

    /**
     * Producer factory for WarehouseAlertMessage with Avro serialization.
     * 
     * @return configured producer factory
     */
    @Bean
    public ProducerFactory<String, WarehouseAlertMessage> warehouseAlertProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Basic Kafka Configuration
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        
        // Schema Registry Configuration
        config.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        config.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true);
        config.put(KafkaAvroSerializerConfig.USE_LATEST_VERSION, true);
        config.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, 
                   io.confluent.kafka.serializers.subject.RecordNameStrategy.class.getName());
        
        // Performance and Reliability Settings
        config.put(ProducerConfig.ACKS_CONFIG, acks);
        config.put(ProducerConfig.RETRIES_CONFIG, retries);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        
        // Batching Configuration for Better Throughput
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        config.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        
        // Timeout Configuration
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
        
        // Client ID for monitoring
        config.put(ProducerConfig.CLIENT_ID_CONFIG, "warehouse-alert-producer");

        log.info("Configured Kafka Producer with bootstrap servers: {}", bootstrapServers);
        log.info("Schema Registry URL: {}", schemaRegistryUrl);
        log.debug("Producer config: acks={}, retries={}, idempotence={}", 
                 acks, retries, enableIdempotence);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate for sending WarehouseAlertMessage.
     * <p>
     * This template is used by the WarehouseAlertProducerService to send messages.
     * 
     * @param producerFactory the producer factory
     * @return configured KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, WarehouseAlertMessage> warehouseAlertKafkaTemplate(
            ProducerFactory<String, WarehouseAlertMessage> producerFactory) {
        
        KafkaTemplate<String, WarehouseAlertMessage> template = new KafkaTemplate<>(producerFactory);
        
        // Enable observation for metrics and tracing
        template.setObservationEnabled(true);
        
        log.info("KafkaTemplate configured for WarehouseAlertMessage");
        
        return template;
    }
}