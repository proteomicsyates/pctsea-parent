package edu.scripps.yates.pctsea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
//@EnableMongoRepositories
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {

		final ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
//		LaunchUtil.launchBrowserInDevelopmentMode(context);

	}

	private static boolean containsArgument(String[] args, String toFind) {
		for (final String arg : args) {
			if (arg.equalsIgnoreCase(toFind)) {
				return true;
			}
		}
		return false;
	}

}
