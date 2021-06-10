package edu.scripps.yates.pctsea.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.scoring.NoThreshold;
import edu.scripps.yates.pctsea.scoring.ScoreThreshold;

public class InputParameters {

	@Override
	public String toString() {
		final StringBuilder scoringSchemasString = new StringBuilder();
		for (final ScoringSchema scoringSchema : scoringSchemas) {
			if (!"".equals(scoringSchemasString.toString())) {
				scoringSchemasString.append("|");
			}
			scoringSchemasString.append(scoringSchema.getScoringMethod() + "-minScore="
					+ scoringSchema.getScoringThreshold().getThresholdValue());
		}

		String string = "InputParameters [writeScoresFile=" + writeScoresFile + ", email=" + email + ", inputDataFile="
				+ inputDataFile + ", minGenesCells=" + minGenesCells + ", min_corr=" + minCorr + ", outputPrefix="
				+ outputPrefix + ", loadRandom=" + loadRandom + ", numPermutations=" + numPermutations
				+ ", cellTypesClassification=" + cellTypeBranch + ", plotNegativeEnriched=" + plotNegativeEnriched
				+ ", uniprotRelease=" + uniprotRelease + ", scoringSchemas=" + scoringSchemasString.toString()
				+ ", inputDataType=" + inputDataType + ", datasets=";
		if (datasets != null) {
			for (final Dataset dataset : datasets) {
				string += dataset.getTag() + "|";
			}

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
	private int minGenesCells;

	@NotNull
	private String outputPrefix;
	private boolean loadRandom;
	private int numPermutations;
	private CellTypeBranch cellTypeBranch;
	private Double minCorr;

	private boolean plotNegativeEnriched;
	private Set<Dataset> datasets;
	private boolean writeScoresFile;
	private String uniprotRelease;
	private List<ScoringSchema> scoringSchemas = new ArrayList<ScoringSchema>();
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
	public static final String MINIMUM_CORRELATION = "min_corr";

	public String getInputDataFile() {
		return inputDataFile;
	}

	public void setInputDataFile(String inputDataFile) {
		this.inputDataFile = inputDataFile;
	}

	public void addScoringSchema(ScoringMethod scoringMethod, double minScore) {
		ScoreThreshold threshold = null;
		if (scoringMethod == ScoringMethod.QUICK_SCORE) {
			threshold = new NoThreshold();
		} else {
			threshold = new ScoreThreshold(minScore);
		}

		final ScoringSchema schema = new ScoringSchema(scoringMethod, threshold);
		scoringSchemas.add(schema);
	}

	public void addScoringSchema(ScoringSchema scoringSchema) {
		scoringSchemas.add(scoringSchema);
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

	public Set<Dataset> getDatasets() {
		return datasets;
	}

	public void setDatasets(Set<Dataset> datasets) {
		this.datasets = datasets;
	}

	public void addDataset(Dataset dataset) {
		datasets.add(dataset);
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

	public List<ScoringSchema> getScoringSchemas() {
		return scoringSchemas;
	}

	public InputDataType getInputDataType() {
		return inputDataType;
	}

	public void setInputDataType(InputDataType inputDataType) {
		this.inputDataType = inputDataType;
	}

	public void setScoringSchemas(List<ScoringSchema> scoringSchemas2) {
		scoringSchemas = scoringSchemas2;

	}

	public Double getMinCorr() {
		return minCorr;
	}

	public void setMinCorr(Double minCorr) {
		this.minCorr = minCorr;
	}

	public int getMinGenesCells() {
		return minGenesCells;
	}

	public void setMinGenesCells(int minGenesCells) {
		this.minGenesCells = minGenesCells;
	}
}
