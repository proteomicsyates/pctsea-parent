package edu.scripps.yates.pctsea.utils.parallel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.springframework.boot.logging.LogLevel;

import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.model.ScoringMethod;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.utils.PCTSEAUtils;
import edu.scripps.yates.utilities.pi.ParIterator;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class EnrichmentWeigthedScoreParallel extends Thread {

	final ParIterator<CellTypeClassification> iterator;
	final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
	final KolmogorovSmirnovTest test = new KolmogorovSmirnovTest();
	final boolean permutatedData;
	final boolean plotNegativeEnrichedCellTypes;
	final String scoreName;
	final File resultsSubfolderForCellTypes;
	final String prefix;
	final boolean compensateWithNegativeSupremum;
	final ScoringMethod scoringMethod;

	public EnrichmentWeigthedScoreParallel(ParIterator<CellTypeClassification> iterator, int numCore,
			List<SingleCell> singleCellList, boolean permutatedData, boolean plotNegativeEnrichedCellTypes,
			String scoreName, File resultsSubfolderForCellTypes, String prefix, boolean compensateWithNegativeSupremum,
			ScoringMethod scoringMethod) {
		this.iterator = iterator;
		this.singleCellList.addAll(singleCellList);

		this.permutatedData = permutatedData;
		this.plotNegativeEnrichedCellTypes = plotNegativeEnrichedCellTypes;
		this.scoreName = scoreName;
		this.resultsSubfolderForCellTypes = resultsSubfolderForCellTypes;
		this.prefix = prefix;
		this.compensateWithNegativeSupremum = compensateWithNegativeSupremum;
		this.scoringMethod = scoringMethod;
	}

	class XYPoint {
		final int x;
		final float y;

		XYPoint(int x, float y) {
			this.x = x;
			this.y = y;
		}

	}

	@Override
	public void run() {
		final TIntIntMap histogramOfNumGenes = new TIntIntHashMap();
		while (iterator.hasNext()) {
			// in case of creating a chart:
			List<XYPoint> scoreSeriesType = null;
			List<XYPoint> scoreSeriesOtherType = null;
			List<XYPoint> supremumLineSeries = null;
			List<XYPoint> secondarySupremumLineSeries = null;
			TDoubleList scoresFromCellType = null;
			TDoubleList scoresForOtherCellTypes = null;
			TFloatList as = null;
			TFloatList bs = null;
			TFloatList differences = null;
			//
			final CellTypeClassification cellType = iterator.next();
			// create chart if not permutated Data, just for real score
			if (!permutatedData) {
				scoreSeriesType = new ArrayList<XYPoint>();
				scoreSeriesOtherType = new ArrayList<XYPoint>();
				supremumLineSeries = new ArrayList<XYPoint>();
				secondarySupremumLineSeries = new ArrayList<XYPoint>();
				// defining two series for score chart

				scoresFromCellType = new TDoubleArrayList();
				scoresForOtherCellTypes = new TDoubleArrayList();

				as = new TFloatArrayList(singleCellList.size());
				bs = new TFloatArrayList(singleCellList.size());
				differences = new TFloatArrayList(singleCellList.size());
			}

			final int cellTypeID = cellType.getCellTypeID();
			float denominatorA = 0.0f;
			float denominatorB = 0.0f;

//			final float denominatorB = 1.0f * (n - nk);
			final List<SingleCell> cellsOfType = new ArrayList<SingleCell>();
			for (final SingleCell singleCell : singleCellList) {
				if (cellTypeID == singleCell.getCellTypeID()) {
					cellsOfType.add(singleCell);
					denominatorA += singleCell.getScoreForRanking();
				} else {
					denominatorB += singleCell.getScoreForRanking();
				}
			}

			final int nk = cellsOfType.size();
			if (nk <= 1) {
				if (!permutatedData) {
					cellType.setEnrichment(Float.NaN, Float.NaN, -1, -1);
				} else {
					cellType.addRandomEnrichment(Float.NaN, Float.NaN);
				}
				continue;
			}

			float supremum = 0.0f;
			final float negativeSupremum = 0.0f;

			float previousA = 0.0f;
			float previousB = 0.0f;
			float numeratorA = 0.0f;
			float numeratorB = 0.0f;
			cellType.clearCellTypeDistribution();
			cellType.clearOutsideCellTypeDistribution();
			int supremumX = 0;

			for (int i = 0; i < singleCellList.size(); i++) {
				final SingleCell singleCell = singleCellList.get(i);

				float a = 0.0f;
				float b = 0.0f;
				if (cellTypeID == singleCell.getCellTypeID()) {
					// this is the difference with the unweigthed, using the correlation, instead of
					// just counting
					numeratorA += singleCell.getScoreForRanking();
					a = numeratorA / denominatorA;
					cellType.addToCellTypeScoreDistribution(
							Double.valueOf(singleCell.getScoreForRanking()).floatValue());
					b = previousB;
					if (scoresFromCellType != null && !Double.isNaN(singleCell.getScoreForRanking())) {
						scoresFromCellType.add(singleCell.getScoreForRanking());
					}
					final int numGenes = singleCell.getGenesUsedForScore().size();
					if (!histogramOfNumGenes.containsKey(numGenes)) {
						histogramOfNumGenes.put(numGenes, 1);
					} else {
						histogramOfNumGenes.put(numGenes, histogramOfNumGenes.get(numGenes) + 1);
					}

				} else {
					numeratorB += singleCell.getScoreForRanking();
					a = previousA;
					b = numeratorB / denominatorB;
					cellType.addOtherCellTypesCorrelationDistribution(
							Double.valueOf(singleCell.getScoreForRanking()).floatValue());
					if (scoresForOtherCellTypes != null && !Double.isNaN(singleCell.getScoreForRanking())) {
						scoresForOtherCellTypes.add(singleCell.getScoreForRanking());
					}
				}
				final float difference = a - b;
				if (!permutatedData) {
					as.add(a);
					bs.add(b);
					scoreSeriesType.add(new XYPoint(i + 1, a));
					scoreSeriesOtherType.add(new XYPoint(i + 1, b));
					differences.add(difference);
				}

				if (Math.abs(difference) > Math.abs(supremum)) {
					supremum = difference;
					supremumX = i + 1;
					if (!permutatedData) {
						supremumLineSeries.clear();
						supremumLineSeries.add(new XYPoint(i + 1, a));
						supremumLineSeries.add(new XYPoint(i + 1, b));
					}
				}

				previousA = a;
				previousB = b;
			}
			if (cellType.getCellTypeCorrelationDistribution() > 1
					&& cellType.getOtherCellTypesCorrelationDistribution() > 1) {

				// now we apply a term factor which is coming from the equation 11 at
				// https://www.pathwaycommons.org/guide/primers/data_analysis/gsea/
				// but also from wikipedia at
				// https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test
				// when samples are large
				final int sizea = cellType.getCellTypeCorrelationDistribution();
				final int sizeb = cellType.getOtherCellTypesCorrelationDistribution();
				final float sqrt = Double.valueOf(Math.sqrt((sizea * sizeb * 1d) / (1d * (sizea + sizeb))))
						.floatValue();
				final float dStatistic = supremum * sqrt;

				if (!permutatedData) {
//					final double ksPvalue = ksPValue(supremum, sizea, sizeb);
					final double ksPvalue = test.kolmogorovSmirnovStatistic(scoresFromCellType.toArray(),
							scoresForOtherCellTypes.toArray());
					cellType.setSizeA(sizea);
					cellType.setSizeB(sizeb);
					final double normalizedSupremumX = 1d * supremumX / singleCellList.size();
					if (!compensateWithNegativeSupremum) {
						cellType.setEnrichment(supremum, dStatistic, supremumX, normalizedSupremumX);
					} else {
						final Pair<Integer, Float> priorNegativeSupremum = lookForPriorNegativeSupremum(supremumX,
								differences);
						if (priorNegativeSupremum != null) {
							supremum = supremum - Math.abs(priorNegativeSupremum.getSecondElement());

						}
						cellType.setEnrichment(supremum, dStatistic, supremumX, normalizedSupremumX);

					}
					cellType.setKSTestPvalue(ksPvalue);

					// final String[] significancies = { "*", "**", "***" };
//					final double[] alphas = { 0.05, 0.01, 0.001 };
//					for (int i = 0; i < alphas.length; i++) {
//						final double alpha = alphas[i];
//						final double cAlpha = Math.sqrt(-Math.log(alpha / 2.0) * (1.0 / 2.0));
//						final double sqrt2 = Math.sqrt((sizea + sizeb) / (sizea * sizeb));
//						if (supremum > cAlpha * sqrt2) {
//							ksSignificancyString = significancies[i];
//						}
//					
					//
					////////////////////////////////////////////////////////////////////////////////
					// we check whether before the actual supremum there was another transition in
					// which the difference was negative and becomes positive
					final int secondaryEnrichmentIndex = lookForSecondaryEnrichmentIndex(supremumX, differences);

					if (secondaryEnrichmentIndex != 0) {

						// then we see if from 0 to that index, we have another positive supremum
						float secondarySupremum = 0.0f;
						previousA = 0.0f;
						previousB = 0.0f;
						numeratorA = 0.0f;
						numeratorB = 0.0f;
						int secondarySupremumX = 0;

						// another more efficient way is to use differences that are already calculated.
						for (int i = 0; i <= secondaryEnrichmentIndex; i++) {
							final float difference = differences.get(i);
							if (difference > 0 && difference > secondarySupremum) {
								secondarySupremum = difference;
								secondarySupremumX = i + 1;
								final float a = as.get(i);
								final float b = bs.get(i);
								secondarySupremumLineSeries.clear();
								secondarySupremumLineSeries.add(new XYPoint(i + 1, a));
								secondarySupremumLineSeries.add(new XYPoint(i + 1, b));
							}
						}
//						System.out.println("sec sup: " + secondarySupremum + "\tsec supX: " + secondarySupremumX);
						secondarySupremum = 0.0f;
						previousA = 0.0f;
						previousB = 0.0f;
						numeratorA = 0.0f;
						numeratorB = 0.0f;
						secondarySupremumX = 0;
						for (int i = 0; i <= secondaryEnrichmentIndex; i++) {
							final SingleCell singleCell = singleCellList.get(i);

							float a = 0.0f;
							float b = 0.0f;

							if (cellTypeID == singleCell.getCellTypeID()) {
								// this is the difference with the unweigthed, using the correlation, instead of
								// just counting
								numeratorA += Math.abs(singleCell.getScoreForRanking());
								a = numeratorA / denominatorA;
								b = previousB;
							} else {
								numeratorB += Math.abs(singleCell.getScoreForRanking());
								a = previousA;
								b = numeratorB / denominatorB;
							}
							// we force to look only to positive ones
							if (a > b) {
								final float difference = a - b;
								if (Math.abs(difference) > Math.abs(secondarySupremum)) {
									secondarySupremum = difference;
									secondarySupremumX = i + 1;

									secondarySupremumLineSeries.clear();
									secondarySupremumLineSeries.add(new XYPoint(i + 1, a));
									secondarySupremumLineSeries.add(new XYPoint(i + 1, b));
								}
							}
							previousA = a;
							previousB = b;
						}
						//
						if (secondarySupremumX != 0) {
							cellType.setSecondaryEnrichment(secondarySupremum, secondarySupremumX);
						}
					}

				} else {
					cellType.addRandomEnrichment(supremum, dStatistic);
				}
			} else {
				if (!permutatedData) {
					cellType.setEnrichment(Float.NaN, Float.NaN, -1, -1);
				} else {
					cellType.addRandomEnrichment(Float.NaN, Float.NaN);
				}
			}

			// only output charts of positive correlations or that have secondary positive
			// enrichment scores
			if (!plotNegativeEnrichedCellTypes && cellType.getEnrichmentScore() < 0f) {
				continue;
			}
			if (!permutatedData) {

				try {
					writeScoreCalculationFile(cellType.getName(), singleCellList.size(), scoreSeriesType,
							scoreSeriesOtherType, supremumLineSeries, secondarySupremumLineSeries);

					// and two series for score chart
					writeScoreDistributionFile(cellType.getName(), scoresFromCellType);
//				final JFreeChart chart = createScoreDistributionChart(cellTypeName, scoresFromCellType, scoreName);
//				cellType.setCorrelationDistributionChart(chart);

					writeNumGenesHistogramFile(cellType.getName(), histogramOfNumGenes);
					// chart with the histogram of number of genes per cell in the cell type
//				final JFreeChart chart2 = createHistogramOfCorrelatingGenesChart(cellTypeName, histogramOfNumGenes);
//				cellType.setHistogramOfCorrelatingGenesChart(chart2);
				} catch (final IOException e) {
					e.printStackTrace();
					PCTSEA.logStatus(
							"Some error occurred while writting files for " + cellTypeID + ": " + e.getMessage(),
							LogLevel.ERROR);
				}
			}

		}
	}

	protected void writeScoreCalculationFile(String cellTypeName, int size, List<XYPoint> scoreSeriesType,
			List<XYPoint> scoreSeriesOtherType, List<XYPoint> supremumLineSeries,
			List<XYPoint> secondarySupremumLineSeries) throws IOException {
		final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(resultsSubfolderForCellTypes, cellTypeName + "_ews",
				prefix, scoringMethod);
		final BufferedWriter buffer = Files.newBufferedWriter(outputTXTFile.toPath(), Charset.forName("UTF-8"));
		buffer.write("-\tcell #\tCumulative Probability [Fn(X)]\n");
		XYPoint previousDataItem = null;
		for (int i = 0; i < scoreSeriesType.size(); i++) {
			final XYPoint dataItem = scoreSeriesType.get(i);
			try {
				final int x = dataItem.x;
				final float y = dataItem.y;
				if (previousDataItem != null && Float.compare(y, previousDataItem.y) == 0) {
					continue;
				}
				if (previousDataItem != null) {
					buffer.write(cellTypeName + "\t" + previousDataItem.x + "\t" + previousDataItem.y + "\n");
				}
				buffer.write(cellTypeName + "\t" + x + "\t" + y + "\n");
			} finally {
				previousDataItem = dataItem;
			}
		}
		previousDataItem = null;
		for (int i = 0; i < scoreSeriesOtherType.size(); i++) {
			final XYPoint dataItem = scoreSeriesOtherType.get(i);
			try {
				final int x = dataItem.x;
				final float y = dataItem.y;
				if (previousDataItem != null && Float.compare(y, previousDataItem.y) == 0) {
					continue;
				}
				if (previousDataItem != null) {
					buffer.write("others\t" + previousDataItem.x + "\t" + previousDataItem.y + "\n");
				}
				buffer.write("others\t" + x + "\t" + y + "\n");
			} finally {
				previousDataItem = dataItem;
			}
		}
		for (int i = 0; i < supremumLineSeries.size(); i++) {
			final XYPoint dataItem = supremumLineSeries.get(i);
			final int x = dataItem.x;
			final float y = dataItem.y;
			buffer.write("supremum\t" + x + "\t" + y + "\n");
		}
		if (secondarySupremumLineSeries != null) {
			for (int i = 0; i < secondarySupremumLineSeries.size(); i++) {
				final XYPoint dataItem = secondarySupremumLineSeries.get(i);
				final int x = dataItem.x;
				final float y = dataItem.y;
				buffer.write("secondary supremum\t" + x + "\t" + y + "\n");
			}
		}
		buffer.close();
	}

	protected void writeNumGenesHistogramFile(String cellTypeName, TIntIntMap histogramOfNumGenes) throws IOException {
		final TIntIntMap histogramOfNumGenesAccumulative = new TIntIntHashMap();
		final TIntList keys = new TIntArrayList(histogramOfNumGenes.keys());
		keys.sort();
		for (int i = 0; i < keys.size(); i++) {
			int accumulativeNumGenes = 0;
			for (int j = i; j < keys.size(); j++) {
				accumulativeNumGenes += histogramOfNumGenes.get(keys.get(j));
			}
			histogramOfNumGenesAccumulative.put(keys.get(i), accumulativeNumGenes);
		}
		final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(resultsSubfolderForCellTypes,
				cellTypeName + "_genes_per_cell_hist", prefix, scoringMethod);
		final BufferedWriter buffer = new BufferedWriter(new FileWriter(outputTXTFile));
		buffer.write("-\t# of genes with " + scoreName + " > threshold\t# cells\n");
		for (final int numGenes : histogramOfNumGenes.keys()) {
			final int frequency = histogramOfNumGenes.get(numGenes);
			buffer.write("# genes\t" + numGenes + "\t" + frequency + "\n");
		}
		for (final int numGenes : histogramOfNumGenesAccumulative.keys()) {
			final int frequency = histogramOfNumGenesAccumulative.get(numGenes);
			buffer.write("# genes or more\t" + numGenes + "\t" + frequency + "\n");
		}
		buffer.close();
	}

	protected void writeScoreDistributionFile(String cellTypeName, TDoubleList scoresFromCellType) throws IOException {
		final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(resultsSubfolderForCellTypes, cellTypeName + "_corr",
				prefix, scoringMethod);
		final BufferedWriter buffer = new BufferedWriter(new FileWriter(outputTXTFile));
		buffer.write(scoreName + "\n");
		for (final double score : scoresFromCellType.toArray()) {
			buffer.write(Double.valueOf(score) + "\n");
		}
		buffer.close();
	}

	/**
	 * When product of sample sizes is less than this value, 2-sample K-S test is
	 * exact
	 */
	protected static final int SMALL_SAMPLE_PRODUCT = 200;

	/**
	 * When product of sample sizes exceeds this value, 2-sample K-S test uses
	 * asymptotic distribution for strict inequality p-value.
	 */
	protected static final int LARGE_SAMPLE_PRODUCT = 10000;

	/**
	 * Default number of iterations used by
	 * {@link #monteCarloP(double, int, int, boolean, int)}
	 */
	protected static final int MONTE_CARLO_ITERATIONS = 500;

	protected double ksPValue(double dStatistic, int a, int b) {

		final long lengthProduct = (long) a * b;
		final boolean strict = true; // checks whether the supremum of random permutations of data are STRICTLY
										// superior to that dStatistic
		if (lengthProduct < SMALL_SAMPLE_PRODUCT) {
			return test.exactP(dStatistic, a, b, strict);
		}
		if (lengthProduct < LARGE_SAMPLE_PRODUCT) {
			final double exactP = test.exactP(dStatistic, a, b, strict);
			final double monteCarloP = test.monteCarloP(dStatistic, a, b, strict, MONTE_CARLO_ITERATIONS);
			final double approximatedP = test.approximateP(dStatistic, a, b);
			return monteCarloP;
		}
		return test.approximateP(dStatistic, a, b);
	}

	/**
	 * Looks for the negative supremum (max difference) prior to the supremumX
	 * 
	 * @param supremumX
	 * @param differences
	 * @return
	 */
	protected Pair<Integer, Float> lookForPriorNegativeSupremum(int supremumX, TFloatList differences) {
		Pair<Integer, Float> ret = null;

		float negativeSupremum = 0;
		int negativeSupremumX = -1;
		for (int i = 0; i < supremumX; i++) {
			final float difference = differences.get(i);
			if (difference < 0) {
				if (Math.abs(difference) > negativeSupremum) {
					negativeSupremum = difference;
					negativeSupremumX = i + 1;
				}
			}
		}
		if (negativeSupremumX != -1) {
			ret = new Pair(negativeSupremumX, negativeSupremum);
		}

		return ret;

	}

	protected int lookForSecondaryEnrichmentIndex(int supremumX, TFloatList differences) {

		int secondaryEnrichmentIndex = 0;
		double previousDifference = 0;
		// going from left to right, that is from less correlated to more correlated,
		// from higher index to lower index.
		// we look for having a positive difference after having a negative difference

		for (int i = supremumX - 1; i >= 0; i--) {

			final double difference = differences.get(i);
			if (difference > 0 && previousDifference < 0.0) {
				secondaryEnrichmentIndex = i;
				break;
			}
			previousDifference = difference;
		}

		return secondaryEnrichmentIndex;
	}

}
