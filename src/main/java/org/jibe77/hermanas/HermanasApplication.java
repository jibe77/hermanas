package org.jibe77.hermanas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class HermanasApplication {

	public static void main(String[] args) {
		SpringApplication.run(HermanasApplication.class, args);
	}
}
