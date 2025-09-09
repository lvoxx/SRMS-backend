package io.github.lvoxx.srms.contactor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication(scanBasePackages = "io.github.lvoxx.srms")
@EnableWebFlux
@EnableR2dbcRepositories
public class ContactorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContactorApplication.class, args);
	}

}
