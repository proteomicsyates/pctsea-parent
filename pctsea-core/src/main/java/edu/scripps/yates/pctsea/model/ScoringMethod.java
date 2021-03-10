package edu.scripps.yates.pctsea.model;

import edu.scripps.yates.utilities.strings.StringUtils;

public enum ScoringMethod {
	PEARSONS_CORRELATION("correlation"), MORPHEUS("morpheus-like-score"), DOT_PRODUCT("dot-product"),
	LIBRARY("library-Xcorr"), MACHINE_LEARNING("ML-score");

	private final String scoreName;

	ScoringMethod(String scoreName) {
		this.scoreName = scoreName;
	}

	public String getScoreName() {
		return scoreName;
	}

	public static String getStringSeparated(String separator) {
		return StringUtils.getSeparatedValueStringFromChars(values(), separator);
	}
}
