package edu.scripps.yates.pctsea.model;

import edu.scripps.yates.utilities.strings.StringUtils;

public enum ScoringMethod {

	PEARSONS_CORRELATION("correlation",
			"Pearson correlation between the abundances of the input list of proteins agains the expression of the same proteins on each single cell of the database",
			true, false),
	SIMPLE_SCORE("simpleScore",
			"simpleScore is the sum of the matching genes and the difference in normalized intensities", true, true),
	DOT_PRODUCT("dot-product", true, true), LIBRARY("library-Xcorr", false, true),
	MACHINE_LEARNING("ML-score", false, true);

	private final String scoreName;
	private final String description;
	private final boolean supported;
	private final boolean experimental;
	public static final String scoringMethodHelperText = "Scoring method used to measure similarities between the input quantitative values and the single cell expressions.";

	ScoringMethod(String scoreName, String description, boolean supported, boolean experimental) {
		this.scoreName = scoreName;
		this.description = description;
		this.supported = supported;
		this.experimental = experimental;
	}

	ScoringMethod(String scoreName, boolean supported, boolean experimental) {
		this(scoreName, null, supported, experimental);
	}

	public String getScoreName() {
		return scoreName;
	}

	public String getDescription() {
		return description;
	}

	public static String getStringSeparated(String separator) {
		return StringUtils.getSeparatedValueStringFromChars(values(), separator);
	}

	public boolean isSupported() {
		return supported;
	}

	public boolean isExperimental() {
		return experimental;
	}
}
