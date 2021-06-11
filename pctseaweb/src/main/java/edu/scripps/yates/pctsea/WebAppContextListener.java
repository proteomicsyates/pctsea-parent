package edu.scripps.yates.pctsea;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.views.analyze.AnalyzeView;

@WebListener
public class WebAppContextListener implements ServletContextListener {
	private final static Logger log = Logger.getLogger(WebAppContextListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		log.info("Context Initialized");
		ServletContextListener.super.contextInitialized(sce);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.info("Context Destroyed");
		// standard thread pool clean up
		final ExecutorService executor = AnalyzeView.executor;
		if (executor != null) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
					executor.shutdownNow();
					if (!executor.awaitTermination(10, TimeUnit.SECONDS))
						System.err.println("Pool did not terminate");
				}
			} catch (final InterruptedException ie) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		log.info("Context Destroyed");
		ServletContextListener.super.contextDestroyed(sce);
	}

}
