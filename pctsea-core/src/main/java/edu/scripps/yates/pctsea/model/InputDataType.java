package edu.scripps.yates.pctsea.model;

public enum InputDataType {
	IP("Immunoprecipitation"), //
	PROTEOME_OF_CELL_LINE("Proteome of a cell line"), //
	PROTEOME_OF_SINGLE_CELL("Proteome of a single cell"), //
	PROTEOME_OF_TISSUE("Proteome of a tissue"), //
	PARTIAL_PROTEOME_OF_CELL_LINE("Partial proteome of cell line"), //
	PARTIAL_PROTEOME_OF_TISSUE("Partial proteome of a tissue");

	private final String description;

	InputDataType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public static String getStringSeparated(String separator) {
		final StringBuilder sb = new StringBuilder();
		for (final InputDataType inputDataType : values()) {
			if (!"".equals(sb.toString())) {
				sb.append(separator);
			}
			sb.append(inputDataType.name());
		}
		return sb.toString();
	}
}
