package io.github.lvoxx.srms.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import io.github.lvoxx.srms.warehouse.config.WarehouseAlertConfig;

/**
 * Main application class for Warehouse Service.
 * <p>
 * Enables Kafka, caching, and scheduling for warehouse management operations.
 * <p>
 * Features:
 * - Kafka integration for alert messaging
 * - Scheduled tasks for periodic alert checks
 * - Caching for statistics performance
 * - Configuration properties binding
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication(scanBasePackages = "io.github.lvoxx.srms")
@EnableWebFlux
@EnableWebSocket
@EnableKafka
@EnableCaching
@EnableScheduling
@EnableR2dbcRepositories
@EnableConfigurationProperties(WarehouseAlertConfig.class)
public class WarehouseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WarehouseServiceApplication.class, args);
	}

}
