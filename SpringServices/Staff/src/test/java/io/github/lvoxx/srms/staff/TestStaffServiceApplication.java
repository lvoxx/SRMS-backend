package io.github.lvoxx.srms.staff;

import org.springframework.boot.SpringApplication;

public class TestStaffServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(StaffServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
