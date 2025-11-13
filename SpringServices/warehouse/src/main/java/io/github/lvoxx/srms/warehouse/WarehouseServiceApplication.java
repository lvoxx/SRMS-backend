package io.github.lvoxx.srms.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication(scanBasePackages = "io.github.lvoxx.srms")
@EnableWebFlux
@EnableWebSocket
@EnableR2dbcRepositories
public class WarehouseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WarehouseServiceApplication.class, args);
	}

}
