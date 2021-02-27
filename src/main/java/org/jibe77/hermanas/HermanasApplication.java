package org.jibe77.hermanas;

import org.jibe77.hermanas.websocket.GreetingController;
import org.jibe77.hermanas.websocket.HelloMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@SpringBootApplication
@EnableRetry
@EnableCaching
@EnableScheduling
@EnableAsync
public class HermanasApplication {

	static Logger logger = LoggerFactory.getLogger(HermanasApplication.class);

	public static void main(String[] args) {
		String userDirectory = new File("").getAbsolutePath();
		logger.info("Current directory : {}.", userDirectory);
		SpringApplication.run(HermanasApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedMethods("GET", "POST", "PUT", "DELETE").allowedHeaders("*")
						.allowedOrigins("*");
			}
		};
	}

	int i = 0;

	@Autowired
	GreetingController greetingController;

	@Autowired
	MessageSendingOperations<String> wsTemplate;

	/**
	 * Generate random numbers publish with WebSocket protocol each 3 seconds.
	 * @return a command line runner.
	 */
	@Bean
	public CommandLineRunner websocketDemo() {
		return (args) -> {
			while (true) {
				try {
					Thread.sleep(3*1000); // Each 3 sec.
					logger.info("publishing test {} ... ", i++);
					greetingController.websocketDemo(new HelloMessage("ceci est un test : " + i));
					this.wsTemplate.convertAndSend("/socket/progress", new HelloMessage("poupou"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}
}
