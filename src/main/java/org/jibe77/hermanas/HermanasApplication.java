package org.jibe77.hermanas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.jibe77.hermanas.security.SecurityConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.io.File;

@SpringBootApplication(exclude = ManagementWebSecurityAutoConfiguration.class)
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
		// CRYPTO-010 : même facteur de travail que l'application. Un hash produit à un facteur
		// plus faible n'est pas bloquant — il est re-haché à la première connexion réussie —
		// mais autant ne pas remettre le compteur à zéro à chaque utilisateur créé à la main.
		String hash = new BCryptPasswordEncoder(SecurityConfig.DEFAULT_BCRYPT_STRENGTH)
				.encode(password);
		System.out.println("{bcrypt}" + hash);
	}
}
