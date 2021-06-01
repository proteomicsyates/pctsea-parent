package edu.scripps.yates.pctsea.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Logger;

import edu.scripps.yates.utilities.properties.PropertiesUtil;

public class PCTSEALocalConfiguration {
	private static final Logger log = Logger.getLogger(PCTSEALocalConfiguration.class.getName());
	/**
	 * You must have this environmental variable pointing to the path of the folder
	 * in which the pctsea.conf properties is located
	 * 
	 * @return
	 */
	public static final String PCTSEA_CONFIGURATION_FOLDER_ENV_NAME = "PCTSEA_CONFIGURATION_PATH";

	public static final String PCTSEA_CONF_FILE_NAME = "pctsea.conf";
	/**************************/
	/* PROPERTIES */
	public static final String resultsPathProperty = "pctsea.results.path";
	public static final String resultsViewerURLProperty = "pctsea.results.viewer.url";
	public static final String fromEmailProperty = "pctsea.from.email";

	/**************************/

	private static File getPCTSEAConfigurationFolder() {

		final String value = System.getenv(PCTSEA_CONFIGURATION_FOLDER_ENV_NAME);
		if (value != null) {
			final File folder = new File(value);
			if (!folder.exists()) {
				throw new PCTSEAConfigurationException("Value of environmental variable '"
						+ PCTSEA_CONFIGURATION_FOLDER_ENV_NAME + "' is '" + value + "' and that folder is not found.");
			}
			return folder;
		}
		throw new PCTSEAConfigurationException(
				"'" + PCTSEA_CONFIGURATION_FOLDER_ENV_NAME + "' environment variable not found");

	}

	public static File getPCTSEAResultsFolder() {
		final String property = getPropertyValue(resultsPathProperty);
		if (property != null && !"".equals(property)) {
			return new File(property);
		}
		return null;
	}

	public static String getPCTSEAResultsViewerURL() {
		final String property = getPropertyValue(resultsViewerURLProperty);
		if (property != null && !"".equals(property)) {
			try {
				final URI uri = new java.net.URL(property).toURI();
				log.log(java.util.logging.Level.INFO, "URL is good: " + uri.toString());
			} catch (MalformedURLException | URISyntaxException e) {
				e.printStackTrace();
				throw new PCTSEAConfigurationException(e);
			}
			return property;
		}
		return null;
	}

	private static String getPropertyValue(String propertyName) {
		try {
			final Properties properties = PropertiesUtil.getProperties(getPCTSEAPropertiesFile());
			if (properties != null) {
				final String property = properties.getProperty(propertyName);
				if (property != null && !"".equals(property)) {
					return property;
				}
			}
		} catch (final Exception e) {
			if (e instanceof PCTSEAConfigurationException) {
				throw (PCTSEAConfigurationException) e;
			}
			e.printStackTrace();
		}
		return null;
	}

	private static File getPCTSEAPropertiesFile() {
		final File confFile = new File(
				getPCTSEAConfigurationFolder().getAbsolutePath() + File.separator + PCTSEA_CONF_FILE_NAME);
		if (!confFile.exists()) {
			throw new PCTSEAConfigurationException("Configuration file '" + PCTSEA_CONF_FILE_NAME
					+ "' not found at path: '" + getPCTSEAConfigurationFolder().getAbsolutePath() + "'");
		}
		return confFile;
	}

	public static String getFromEmail() {
		final String property = getPropertyValue(fromEmailProperty);
		if (property != null && !"".equals(property)) {
			return property;
		}
		return null;
	}
}
