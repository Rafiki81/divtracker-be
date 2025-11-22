package com.rafiki18.divtracker_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DivtrackerBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(DivtrackerBeApplication.class, args);
	}

}
