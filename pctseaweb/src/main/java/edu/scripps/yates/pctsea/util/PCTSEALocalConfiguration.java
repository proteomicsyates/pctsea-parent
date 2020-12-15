package edu.scripps.yates.pctsea.util;

import java.io.File;
import java.util.Properties;

import edu.scripps.yates.utilities.properties.PropertiesUtil;

public class PCTSEALocalConfiguration {
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

	/**************************/

	private static File getPCTSEAConfigurationFolder() {
		final String value = System.getenv(PCTSEA_CONFIGURATION_FOLDER_ENV_NAME);
		if (value != null) {
			return new File(value);
		}
		return null;
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
			e.printStackTrace();
		}
		return null;
	}

	private static File getPCTSEAPropertiesFile() {
		return new File(getPCTSEAConfigurationFolder().getAbsolutePath() + File.separator + PCTSEA_CONF_FILE_NAME);
	}
}
