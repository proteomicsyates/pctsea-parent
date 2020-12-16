package edu.scripps.yates.pctsea.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jfree.chart.JFreeChart;

import edu.scripps.yates.pctsea.correlation.CorrelationThreshold;
import edu.scripps.yates.pctsea.model.charts.ChartsGenerated;
import edu.scripps.yates.pctsea.utils.PCTSEAUtils;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.THashMap;

public class CellTypeClassification {
	private final String name;
	private final double hypergeometricPValue;
	private float enrichmentUnweightedScore;
	private float enrichmentScore = Float.NaN;
	private long numCellsOfType;
	private long numCellsOfTypePassingCorrelationThreshold;
	private final List<SingleCell> singleCellsOfThisType = new ArrayList<SingleCell>();

	public List<SingleCell> getSingleCellsOfThisType() {
		return singleCellsOfThisType;
	}

	private final TFloatList cellTypeCorrelationDistribution = new TFloatArrayList();
	private final TFloatList otherCellTypesCorrelationDistribution = new TFloatArrayList();
	private double enrichmentSignificance;
	private float casimirsEnrichmentScore;
	private JFreeChart correlationDistributionChart;
	private JFreeChart chartScoreCalculation;
	private String significancyString;
	private Float umapClusteringX;
	private Float umapClusteringY;
	private JFreeChart histogramOfCorrelatingGenesChart;
	private int supremumX;
	private Float secondaryEnrichmentScore;
	private Integer secondarySupremumX;
	private float dStatistic;
	private double ksPvalue;
	private Float normalizedEnrichmentScore;
	private double enrichmentFDR;
	private TFloatList randomKSTestStatistics;
	private TFloatList randomEnrichmentScores;
	private TFloatList normalizedRandomEnrichmentScores;
	private double normalizedSupremumX;
	private int sizeA;
	private int sizeB;
	private double ksTestCorrectedPValue;

	public long getNumCellsOfTypePassingCorrelationThreshold() {
		return numCellsOfTypePassingCorrelationThreshold;
	}

	public CellTypeClassification(String name, double hypergeometricPValue) {
		super();
		this.name = name;
		this.hypergeometricPValue = hypergeometricPValue;
	}

	public String getName() {
		return name;
	}

	public double getHypergeometricPValue() {
		return hypergeometricPValue;
	}

	public double getEnrichmentUnweightedScore() {
		return enrichmentUnweightedScore;
	}

	public void setEnrichmentUnweightedScore(float enrichmentUnweightedScore) {
		this.enrichmentUnweightedScore = enrichmentUnweightedScore;
	}

	public float getEnrichmentScore() {
		return enrichmentScore;
	}

	public void setEnrichment(float supremum, float dStatistic, int supremumX, double normalizedSupremumX) {
		this.enrichmentScore = supremum;
		this.dStatistic = dStatistic;
		this.supremumX = supremumX;
		this.normalizedSupremumX = normalizedSupremumX;
	}

	public void addRandomEnrichment(float supremum, float dStatistic) {
		addRandomEnrichmentScore(supremum);
		addRandomKSTestDStatistic(dStatistic);
	}

	private void addRandomKSTestDStatistic(float dStatistic2) {
		if (randomKSTestStatistics == null) {
			randomKSTestStatistics = new TFloatArrayList();
		}
		randomKSTestStatistics.add(dStatistic2);
	}

	private void addRandomEnrichmentScore(float supremum) {
		if (randomEnrichmentScores == null) {
			randomEnrichmentScores = new TFloatArrayList();
		}
		randomEnrichmentScores.add(supremum);
	}

	public TFloatList getRandomEnrichmentScores() {
		return this.randomEnrichmentScores;
	}

	public TFloatList getNormalizedRandomEnrichmentScores() {
		return this.normalizedRandomEnrichmentScores;
	}

	public TFloatList getRandomKSTestDStatistics() {
		return this.randomKSTestStatistics;
	}

	public long getNumCellsOfType() {
		return numCellsOfType;
	}

	public void setNumCellsOfType(long numCellsOfType) {
		this.numCellsOfType = numCellsOfType;
	}

	public void setNumCellsOfTypePassingCorrelationThreshold(long numCellsOfTypeWithPositiveCorrelation) {
		this.numCellsOfTypePassingCorrelationThreshold = numCellsOfTypeWithPositiveCorrelation;

	}

	public void setSingleCells(List<SingleCell> cellsOfCellType) {
		this.singleCellsOfThisType.addAll(cellsOfCellType);
	}

	public List<GeneOccurrence> getRankingOfGenesThatContributedToTheCorrelation(
			CorrelationThreshold correlationThreshold) {
		final Map<String, GeneOccurrence> map = new THashMap<String, GeneOccurrence>();
		final List<SingleCell> cells = correlationThreshold.getSingleCellsPassingThreshold(singleCellsOfThisType);
		for (final SingleCell singleCell : cells) {
			final Set<String> genes = singleCell.getGenesForCorrelation().stream().collect(Collectors.toSet());
			for (final String gene : genes) {
				if (!map.containsKey(gene)) {
					map.put(gene, new GeneOccurrence(gene));
				}
				map.get(gene).incrementOccurrence();
			}
		}
		final List<GeneOccurrence> geneOccurrences = new ArrayList<GeneOccurrence>();
		geneOccurrences.addAll(map.values());
		Collections.sort(geneOccurrences, new Comparator<GeneOccurrence>() {

			@Override
			public int compare(GeneOccurrence o1, GeneOccurrence o2) {
				final int ret = Integer.compare(o2.getOccurrence(), o1.getOccurrence());
				if (ret != 0) {
					return ret;
				} else {
					return o1.getGene().compareTo(o2.getGene());
				}
			}
		});
		return geneOccurrences;
	}

	public String getStringOfRankingOfGenesThatContributedToTheCorrelation(CorrelationThreshold correlationThreshold) {
		final List<GeneOccurrence> geneOccurrences = getRankingOfGenesThatContributedToTheCorrelation(
				correlationThreshold);
		final StringBuilder sb = new StringBuilder();
		for (final GeneOccurrence geneOccurrence : geneOccurrences) {
			if (!"".equals(sb.toString())) {
				sb.append(",");
			}
			sb.append(geneOccurrence.getGene() + "[" + geneOccurrence.getOccurrence() + "]");
		}
		return sb.toString();
	}

	public void addToCellTypeCorrelationDistribution(float correlationValue) {
		this.cellTypeCorrelationDistribution.add(correlationValue);
	}

	public void addOtherCellTypesCorrelationDistribution(float b) {
		this.otherCellTypesCorrelationDistribution.add(b);
	}

	public TFloatList getCellTypeCorrelationDistribution() {
		return cellTypeCorrelationDistribution;
	}

	public TFloatList getOtherCellTypesCorrelationDistribution() {
		return otherCellTypesCorrelationDistribution;
	}

	public void setEnrichmentSignificance(double pvalue) {
		this.enrichmentSignificance = pvalue;
	}

	public double getEnrichmentSignificance() {
		return enrichmentSignificance;
	}

	public void clearCellTypeDistribution() {
		if (this.cellTypeCorrelationDistribution != null) {
			this.cellTypeCorrelationDistribution.clear();
		}

	}

	public void clearOutsideCellTypeDistribution() {
		if (this.otherCellTypesCorrelationDistribution != null) {
			this.otherCellTypesCorrelationDistribution.clear();
		}
	}

	public float getCasimirsEnrichmentScore() {
		return casimirsEnrichmentScore;
	}

	public void setCasimirsEnrichmentScore(float casimirsEnrichmentScore2) {
		this.casimirsEnrichmentScore = casimirsEnrichmentScore2;
	}

	@Override
	public String toString() {
		return "'" + getName() + "' [ews:" + enrichmentScore + ", sup:" + enrichmentScore + ", supX:" + supremumX
				+ ", sig:" + enrichmentSignificance + "]";
	}

	public void setCorrelationDistributionChart(JFreeChart chart) {
		this.correlationDistributionChart = chart;
	}

	public void setEnrichmentScoreCalculationLineChart(JFreeChart chartScoreCalculation) {
		this.chartScoreCalculation = chartScoreCalculation;

	}

	public JFreeChart getEnrichmentScorecalculationLineChart() {
		return this.chartScoreCalculation;
	}

	public List<File> saveCharts(File resultsSubFolder, String prefix, boolean generatePDFCharts) throws IOException {
		final List<File> txtfiles = new ArrayList<File>();
		txtfiles.add(saveChartToFileAndToBufferedImage(this.correlationDistributionChart, this.getName() + "_corr",
				resultsSubFolder, prefix, generatePDFCharts));
		txtfiles.add(saveChartToFileAndToBufferedImage(this.chartScoreCalculation, this.getName() + "_ews",
				resultsSubFolder, prefix, generatePDFCharts));
		txtfiles.add(saveChartToFileAndToBufferedImage(this.histogramOfCorrelatingGenesChart,
				this.getName() + "_genes_per_cell_hist", resultsSubFolder, prefix, generatePDFCharts));
		// remove any null
		final List<File> ret = txtfiles.stream().filter(f -> f != null).collect(Collectors.toList());
		return ret;
	}

	private String getSafeName(String name2) {
		if (name2.contains(File.separator)) {
			return name2.replace(File.separator, "_");
		}
		return name2;
	}

	private File saveChartToFileAndToBufferedImage(JFreeChart chart, String fileName, File resultsSubFolder,
			String prefix, boolean generatePDFCharts) throws IOException {
		if (chart == null) {
			return null;
		}

		final File txtFile = PCTSEAUtils.writeTXTFileForChart(chart, resultsSubFolder, prefix, fileName);
		if (generatePDFCharts) {
			final File chartFile = PCTSEAUtils.getChartPDFFile(resultsSubFolder, fileName, prefix);
			final int width = 500;
			final int height = 500;
			ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, width, height, false, chartFile);
		}
		return txtFile;
	}

	public void setKSTestSignificancyString(String significancyString) {
		this.significancyString = significancyString;
	}

	public String getSignificancyString() {
		return significancyString != null ? significancyString : "";
	}

	public void setUmapClustering(Float x, Float y) {

		this.umapClusteringX = x;
		this.umapClusteringY = y;
	}

	public Float getUmapClusteringX() {
		return this.umapClusteringX;
	}

	public Float getUmapClusteringY() {
		return this.umapClusteringY;
	}

	/**
	 * number of genes per cell in the cell type
	 * 
	 * @param chart
	 */
	public void setHistogramOfCorrelatingGenesChart(JFreeChart chart) {
		this.histogramOfCorrelatingGenesChart = chart;

	}

	public int getSupremumX() {
		return supremumX;
	}

	public Float getSecondaryEnrichmentScore() {
		return secondaryEnrichmentScore;
	}

	public void setSecondaryEnrichment(float secondaryEnrichmentWeigthedScore, int secondarySupremumX) {
		this.secondaryEnrichmentScore = secondaryEnrichmentWeigthedScore;
		this.secondarySupremumX = secondarySupremumX;
	}

	public Integer getSecondarySupremumX() {
		return secondarySupremumX;
	}

	public float getKSTestDStatistic() {
		return this.dStatistic;
	}

	public void setKSTestPvalue(double ksPvalue) {
		this.ksPvalue = ksPvalue;
	}

	public double getKSTestPvalue() {
		return this.ksPvalue;
	}

	/**
	 * Calculates the normalized enrichment scores by dividing the real score by the
	 * expected random score (average of the random scores).<br>
	 * Note that after calling this function, the random scores get also normalized.
	 * 
	 * @return
	 */
	public float getNormalizedEnrichmentScore() {
		if (this.normalizedEnrichmentScore == null) {
			if (Float.isNaN(enrichmentScore)) {
				this.normalizedEnrichmentScore = Float.NaN;
				return this.normalizedEnrichmentScore;
			}
			final TFloatList randomScores = getRandomEnrichmentScores();
			final TFloatList positiveRandomScores = new TFloatArrayList();
			final TFloatList negativeRandomScores = new TFloatArrayList();
			for (final float randomScore : randomScores.toArray()) {
				if (randomScore >= 0f) {
					positiveRandomScores.add(randomScore);
				} else if (randomScore < 0f) {
					negativeRandomScores.add(randomScore);
				}
			}
			float expectedRandomScore = Float.NaN;
			if (enrichmentScore >= 0f) {
				expectedRandomScore = Math.abs(Maths.mean(positiveRandomScores));
				normalizedEnrichmentScore = enrichmentScore / expectedRandomScore;
			} else {
				expectedRandomScore = Math.abs(Maths.mean(negativeRandomScores));
				normalizedEnrichmentScore = enrichmentScore / expectedRandomScore;
			}
			// now, we normalize also the random scores
			final TFloatList normalizedRandomScores = new TFloatArrayList();
			for (final float randomScore : getRandomEnrichmentScores().toArray()) {
				final float normalizedRandomScore = randomScore / expectedRandomScore;
				normalizedRandomScores.add(normalizedRandomScore);
			}
			this.normalizedRandomEnrichmentScores = new TFloatArrayList();
			this.normalizedRandomEnrichmentScores.addAll(normalizedRandomScores);
		}
		return this.normalizedEnrichmentScore;
	}

	public void setEnrichmentFDR(double fdr) {
		this.enrichmentFDR = fdr;
	}

	public double getEnrichmentFDR() {
		return this.enrichmentFDR;
	}

	public double getNormalizedSupremumX() {
		return normalizedSupremumX;
	}

	public int getSizeA() {
		return sizeA;
	}

	public int getSizeB() {
		return sizeB;
	}

	public void setSizeA(int sizeA) {
		this.sizeA = sizeA;
	}

	public void setSizeB(int sizeB) {
		this.sizeB = sizeB;
	}

	public void setKSTestCorrectedPvalue(double ksTestCorrectedPValue) {
		this.ksTestCorrectedPValue = ksTestCorrectedPValue;
	}

	public double getKSTestCorrectedPvalue() {
		return ksTestCorrectedPValue;
	}
}
