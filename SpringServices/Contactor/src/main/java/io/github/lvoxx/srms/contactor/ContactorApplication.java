package io.github.lvoxx.srms.contactor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
@EnableR2dbcRepositories
@ComponentScan("io.github.lvoxx.srms")
public class ContactorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContactorApplication.class, args);
	}

}
