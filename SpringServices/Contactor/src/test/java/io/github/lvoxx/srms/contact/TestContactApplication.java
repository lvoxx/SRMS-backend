package io.github.lvoxx.srms.contact;

import org.springframework.boot.SpringApplication;

public class TestContactApplication {

	public static void main(String[] args) {
		SpringApplication.from(ContactApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
