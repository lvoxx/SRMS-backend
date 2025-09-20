package io.github.lvoxx.srms.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.lvoxx.srms")
public class ReportingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportingApplication.class, args);
	}

}
