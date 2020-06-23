package org.jibe77.hermanas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableCaching
@EnableScheduling
public class HermanasApplication {

	public static void main(String[] args) {
		SpringApplication.run(HermanasApplication.class, args);
	}
}
