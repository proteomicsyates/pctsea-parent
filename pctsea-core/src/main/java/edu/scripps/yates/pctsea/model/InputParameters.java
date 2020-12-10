package edu.scripps.yates.pctsea.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

public class InputParameters {
	@Override
	public String toString() {
		return "InputParameters [writeCorrelationsFile=" + writeCorrelationsFile + ", email=" + email
				+ ", inputDataFile=" + inputDataFile + ", minCorrelation=" + minCorrelation + ", minGenesCells="
				+ minGenesCells + ", outputPrefix=" + outputPrefix + ", loadRandom=" + loadRandom + ", numPermutations="
				+ numPermutations + ", cellTypesClassification=" + cellTypesClassification + ", generateCharts="
				+ generateCharts + ", minCellsPerCellType=" + minCellsPerCellType + ", plotNegativeEnriched="
				+ plotNegativeEnriched + ", datasets=" + getSeparatedValueString(datasets, ",") + "]";
	}

	private String getSeparatedValueString(Set<String> datasets2, String separator) {
		final StringBuilder sb = new StringBuilder();
		if (datasets2 != null) {
			for (final String dataset : datasets2) {
				if (!"".equals(sb.toString())) {
					sb.append(separator);
				}
				sb.append(dataset);
			}
		}
		return sb.toString();
	}

	private String email;
	private String inputDataFile;
	private double minCorrelation;
	private int minGenesCells;
	@NotNull
	private String outputPrefix;
	private boolean loadRandom;
	private int numPermutations;
	private String cellTypesClassification;
	private boolean generateCharts;
	private int minCellsPerCellType;
	private boolean plotNegativeEnriched;
	private Set<String> datasets;
	private boolean writeCorrelationsFile;
	public static final String EMAIL = "email";
	public static final String OUT = "out";
	public static final String PERM = "perm";
	public static final String EEF = "eef";
	public static final String CHARTS = "charts";
	public static final String MIN_CORRELATION = "min_correlation";
	public static final String MIN_GENES_CELLS = "min_genes_cells";
	public static final String CELL_TYPES_CLASSIFICATION = "cell_types_classification";
	public static final String LOAD_RANDOM = "load_random";
	public static final String PLOT_NEGATIVE_ENRICHED = "plot_negative_enriched";
	public static final String MIN_CELLS_PER_CELL_TYPE = "min_cells_per_cell_type";
	public static final String DATASETS = "datasets";
	public static final String WRITE_CORRELATIONS = "write_correlations";

	public String getInputDataFile() {
		return inputDataFile;
	}

	public void setInputDataFile(String inputDataFile) {
		this.inputDataFile = inputDataFile;
	}

	public double getMinCorrelation() {
		return minCorrelation;
	}

	public void setMinCorrelation(double minCorrelation) {
		this.minCorrelation = minCorrelation;
	}

	public int getMinGenesCells() {
		return minGenesCells;
	}

	public void setMinGenesCells(int minGenesCells) {
		this.minGenesCells = minGenesCells;
	}

	public String getOutputPrefix() {
		return outputPrefix;
	}

	public void setOutputPrefix(String outputPrefix) {
		this.outputPrefix = outputPrefix;
	}

	public boolean isLoadRandom() {
		return loadRandom;
	}

	public void setLoadRandom(boolean loadRandom) {
		this.loadRandom = loadRandom;
	}

	public int getNumPermutations() {
		return numPermutations;
	}

	public void setNumPermutations(int numPermutations) {
		this.numPermutations = numPermutations;
	}

	public String getCellTypesClassification() {
		return cellTypesClassification;
	}

	public void setCellTypesClassification(String cellTypesClassification) {
		this.cellTypesClassification = cellTypesClassification;
	}

	public boolean isGenerateCharts() {
		return generateCharts;
	}

	public void setGenerateCharts(boolean generateCharts) {
		this.generateCharts = generateCharts;
	}

	public int getMinCellsPerCellType() {
		return minCellsPerCellType;
	}

	public void setMinCellsPerCellType(int minCellsPerCellType) {
		this.minCellsPerCellType = minCellsPerCellType;
	}

	public boolean isPlotNegativeEnriched() {
		return plotNegativeEnriched;
	}

	public void setPlotNegativeEnriched(boolean plotNegativeEnriched) {
		this.plotNegativeEnriched = plotNegativeEnriched;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Set<String> getDatasets() {
		return datasets;
	}

	public void setDatasets(Set<String> datasets) {
		this.datasets = datasets;
	}

	public boolean isWriteCorrelationsFile() {
		return writeCorrelationsFile;
	}

	public void setWriteCorrelationsFile(boolean writeCorrelationsFile) {
		this.writeCorrelationsFile = writeCorrelationsFile;
	}
}
