package io.github.lvoxx.srms.kitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.lvoxx.srms")
public class KitchenApplication {

	public static void main(String[] args) {
		SpringApplication.run(KitchenApplication.class, args);
	}

}
