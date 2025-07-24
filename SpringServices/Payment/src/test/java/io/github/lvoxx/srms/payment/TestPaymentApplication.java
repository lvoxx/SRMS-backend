package io.github.lvoxx.srms.payment;

import org.springframework.boot.SpringApplication;

public class TestPaymentApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
