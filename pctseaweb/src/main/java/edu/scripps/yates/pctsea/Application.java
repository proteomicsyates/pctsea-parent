package edu.scripps.yates.pctsea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.vaadin.artur.helpers.LaunchUtil;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {

		LaunchUtil.launchBrowserInDevelopmentMode(SpringApplication.run(Application.class, args));

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
