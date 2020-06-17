package org.jibe77.hermanas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableCaching
public class HermanasApplication {

	public static void main(String[] args) {
		SpringApplication.run(HermanasApplication.class, args);
	}
}
