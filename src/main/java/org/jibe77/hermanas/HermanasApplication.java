package org.jibe77.hermanas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.io.File;

@SpringBootApplication
@EnableRetry
@EnableCaching
@EnableScheduling
@EnableAsync
public class HermanasApplication {

	private static final Logger logger = LoggerFactory.getLogger(HermanasApplication.class);

	public static void main(String[] args) {
		if (args.length > 0 && "--hash".equals(args[0])) {
			runHashCli(args);
			return;
		}
		logger.info("Current directory : {}.", new File("").getAbsolutePath());
		SpringApplication.run(HermanasApplication.class, args);
	}

	private static void runHashCli(String[] args) {
		String password;
		if (args.length >= 2) {
			password = args[1];
		} else {
			Console console = System.console();
			if (console == null) {
				System.err.println("No console available. Usage: java -jar hermanas.jar --hash <password>");
				System.exit(2);
				return;
			}
			char[] chars = console.readPassword("Password: ");
			password = new String(chars);
		}
		String hash = new BCryptPasswordEncoder().encode(password);
		System.out.println("{bcrypt}" + hash);
	}
}
