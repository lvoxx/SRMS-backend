package io.github.lvoxx.srms.kitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.reactive.config.EnableWebFlux;


/**
 * The Kitchen service is responsible for the following tasks:
 * <ul>
 *     <li>Receives orders from the order-service.</li>
 *     <li>Manages received orders and processes dishes.</li>
 *     <li>Manages kitchen utensils, ingredients, and supplies.</li>
 *     <li>Provides statistics on kitchen activity.</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "io.github.lvoxx.srms")
@EnableWebFlux
@EnableKafka
@EnableCaching
@EnableR2dbcRepositories
public class KitchenApplication {

	public static void main(String[] args) {
		SpringApplication.run(KitchenApplication.class, args);
	}

}
