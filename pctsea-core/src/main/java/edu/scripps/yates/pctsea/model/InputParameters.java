package edu.scripps.yates.pctsea.model;

import javax.validation.constraints.NotNull;

import edu.scripps.yates.pctsea.db.Dataset;

public class InputParameters {
	@Override
	public String toString() {
		String string = "InputParameters [writeScoresFile=" + writeScoresFile + ", email=" + email + ", inputDataFile="
				+ inputDataFile + ", minScore=" + minScore + ", minGenesCells=" + minGenesCells + ", outputPrefix="
				+ outputPrefix + ", loadRandom=" + loadRandom + ", numPermutations=" + numPermutations
				+ ", cellTypesClassification=" + cellTypeBranch + ", plotNegativeEnriched=" + plotNegativeEnriched
				+ ", uniprotRelease=" + uniprotRelease + ", scoringMethod=" + scoringMethod + ", inputDataType="
				+ inputDataType + ", dataset=";
		if (dataset != null) {
			string += dataset.getTag();
		} else {
			string += "not specified";
		}
		return string + "]";
	}

//	private String getSeparatedValueString(Set<String> datasets2, String separator) {
//		final StringBuilder sb = new StringBuilder();
//		if (datasets2 != null) {
//			for (final String dataset : datasets2) {
//				if (!"".equals(sb.toString())) {
//					sb.append(separator);
//				}
//				sb.append(dataset);
//			}
//		}
//		return sb.toString();
//	}

	private String email;
	private String inputDataFile;
	private double minScore;
	private int minGenesCells;
	@NotNull
	private String outputPrefix;
	private boolean loadRandom;
	private int numPermutations;
	private CellTypeBranch cellTypeBranch;
	private boolean plotNegativeEnriched;
	private Dataset dataset;
	private boolean writeScoresFile;
	private String uniprotRelease;
	private ScoringMethod scoringMethod = ScoringMethod.PEARSONS_CORRELATION; // by default
	private InputDataType inputDataType;
	public static final String EMAIL = "email";
	public static final String OUT = "out";
	public static final String PERM = "perm";
	public static final String EEF = "eef";
	public static final String MIN_SCORE = "min_score";
	public static final String MIN_GENES_CELLS = "min_genes_cells";
	public static final String CELL_TYPES_CLASSIFICATION = "cell_types_classification";
	public static final String LOAD_RANDOM = "load_random";
	public static final String PLOT_NEGATIVE_ENRICHED = "plot_negative_enriched";
	public static final String DATASETS = "datasets";
	public static final String WRITE_SCORES = "write_scores";
	public static final String UNIPROT_RELEASE = "uniprot_release";
	public static final String SCORING_METHOD = "scoring_method";
	public static final String INPUT_DATA_TYPE = "input_data_type";

	public String getInputDataFile() {
		return inputDataFile;
	}

	public void setInputDataFile(String inputDataFile) {
		this.inputDataFile = inputDataFile;
	}

	public double getMinScore() {
		return minScore;
	}

	public void setMinScore(double minScore) {
		this.minScore = minScore;
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

	public CellTypeBranch getCellTypeBranch() {
		return cellTypeBranch;
	}

	public void setCellTypeBranch(CellTypeBranch cellTypeBranch) {
		this.cellTypeBranch = cellTypeBranch;
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

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public boolean isWriteScoresFile() {
		return writeScoresFile;
	}

	public void setWriteScoresFile(boolean writeScoresFile) {
		this.writeScoresFile = writeScoresFile;
	}

	public String getUniprotRelease() {
		return uniprotRelease;
	}

	public void setUniprotRelease(String uniprotRelease) {
		this.uniprotRelease = uniprotRelease;
	}

	public ScoringMethod getScoringMethod() {
		return scoringMethod;
	}

	public void setScoringMethod(ScoringMethod scoringMethod) {
		this.scoringMethod = scoringMethod;
	}

	public InputDataType getInputDataType() {
		return inputDataType;
	}

	public void setInputDataType(InputDataType inputDataType) {
		this.inputDataType = inputDataType;
	}
}
