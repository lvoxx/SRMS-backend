package io.github.lvoxx.srms.kitchen;

import org.springframework.boot.SpringApplication;

public class TestKitchenApplication {

	public static void main(String[] args) {
		SpringApplication.from(KitchenApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
