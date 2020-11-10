package edu.scripps.yates.pctsea.utils.parallel;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.model.charts.IntegerCategoryItemLabelGenerator;
import edu.scripps.yates.pctsea.utils.PCTSEAUtils;
import edu.scripps.yates.utilities.maths.Histogram;
import edu.scripps.yates.utilities.pi.ParIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class EnrichmentWeigthedScoreParallel extends Thread {

	private final ParIterator<CellTypeClassification> iterator;
	private final int numCore;
	private final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
	private final KolmogorovSmirnovTest test = new KolmogorovSmirnovTest();
	private final CellTypeBranch cellTypeBranch;
	private final boolean permutatedData;
	private final boolean generateCharts;
	private final int minCellsPerCellTypeForPDF;
	private final boolean plotNegativeEnrichedCellTypes;

	public EnrichmentWeigthedScoreParallel(ParIterator<CellTypeClassification> iterator, int numCore,
			List<SingleCell> singleCellList, CellTypeBranch cellTypeBranch, boolean permutatedData,
			boolean generateCharts2, int minCellsPerCellTypeForPDF, boolean plotNegativeEnrichedCellTypes) {
		this.iterator = iterator;
		this.numCore = numCore;
		this.singleCellList.addAll(singleCellList);
		// sort by correlation from higher to lower
		PCTSEAUtils.sortByDescendingCorrelation(this.singleCellList);
		this.cellTypeBranch = cellTypeBranch;
		this.permutatedData = permutatedData;
		this.generateCharts = generateCharts2;
		this.minCellsPerCellTypeForPDF = minCellsPerCellTypeForPDF;
		this.plotNegativeEnrichedCellTypes = plotNegativeEnrichedCellTypes;
	}

	@Override
	public void run() {
		final DecimalFormat format = new DecimalFormat("#.###");
//		System.out.println("Calculating weigthed enrichment score and KS statistics (core " + numCore + ")...");
		final int n = singleCellList.size();
		final TIntIntMap histogramOfNumGenes = new TIntIntHashMap();
		while (iterator.hasNext()) {
			// in case of creating a chart:
			XYSeries scoreSeriesType = null;
			XYSeries scoreSeriesOtherType = null;
			XYSeries supremumLineSeries = null;
			XYSeries secondarySupremumLineSeries = null;
			TDoubleList corrFrequencyType = null;
			TDoubleList corrFrequencyOthers = null;

			final TDoubleList fx = new TDoubleArrayList();
			final TDoubleList fy = new TDoubleArrayList();
			//
			final CellTypeClassification cellType = iterator.next();
			// create chart if not permutated Data, just for real score
			if (generateCharts && !permutatedData) {

				// defining two series for score chart
				scoreSeriesType = new XYSeries(cellType.getName());
				scoreSeriesOtherType = new XYSeries("others");
				supremumLineSeries = new XYSeries("supremum");
				corrFrequencyType = new TDoubleArrayList();
				corrFrequencyOthers = new TDoubleArrayList();
			}

			final String cellTypeName = cellType.getName();
			float denominatorA = 0.0f;
			float denominatorB = 0.0f;
			final TDoubleList differences = new TDoubleArrayList();

//			final float denominatorB = 1.0f * (n - nk);
			final List<SingleCell> cellsOfType = new ArrayList<SingleCell>();
			for (final SingleCell singleCell : singleCellList) {
				if (cellTypeName.equals(singleCell.getCellType(this.cellTypeBranch))) {
					cellsOfType.add(singleCell);
					denominatorA += Double.valueOf(singleCell.getCorrelation()).floatValue();
				} else {
					denominatorB += Double.valueOf(singleCell.getCorrelation()).floatValue();
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
				if (cellTypeName.equals(singleCell.getCellType(this.cellTypeBranch))) {
					// this is the difference with the unweigthed, using the correlation, instead of
					// just counting
					numeratorA += singleCell.getCorrelation();
					fx.add(singleCell.getCorrelation());
					a = numeratorA / denominatorA;
					cellType.addToCellTypeCorrelationDistribution(
							Double.valueOf(singleCell.getCorrelation()).floatValue());
					b = previousB;
					if (generateCharts && corrFrequencyType != null && !Double.isNaN(singleCell.getCorrelation())) {
						corrFrequencyType.add(singleCell.getCorrelation());
					}
					final int numGenes = singleCell.getGenesForCorrelation().size();
					if (!histogramOfNumGenes.containsKey(numGenes)) {
						histogramOfNumGenes.put(numGenes, 1);
					} else {
						histogramOfNumGenes.put(numGenes, histogramOfNumGenes.get(numGenes) + 1);
					}

				} else {
					fy.add(singleCell.getCorrelation());
					numeratorB += singleCell.getCorrelation();
					a = previousA;
					b = numeratorB / denominatorB;
					cellType.addOtherCellTypesCorrelationDistribution(
							Double.valueOf(singleCell.getCorrelation()).floatValue());
					if (generateCharts && corrFrequencyOthers != null && !Double.isNaN(singleCell.getCorrelation())) {
						corrFrequencyOthers.add(singleCell.getCorrelation());
					}
				}
				if (generateCharts && !permutatedData) {
					scoreSeriesType.add(i + 1, a);
					scoreSeriesOtherType.add(i + 1, b);
				}
				final float difference = a - b;
				differences.add(difference);
				if (Math.abs(difference) > Math.abs(supremum)) {
					supremum = difference;
					supremumX = i + 1;
					if (generateCharts && !permutatedData) {
						supremumLineSeries.clear();
						supremumLineSeries.add(i + 1, a);
						supremumLineSeries.add(i + 1, b);
					}
				}
				previousA = a;
				previousB = b;
			}
			if (cellType.getCellTypeCorrelationDistribution().size() > 1
					&& cellType.getOtherCellTypesCorrelationDistribution().size() > 1) {

				// now we apply a term factor which is coming from the equation 11 at
				// https://www.pathwaycommons.org/guide/primers/data_analysis/gsea/
				// but also from wikipedia at
				// https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test
				// when samples are large
				final int sizea = cellType.getCellTypeCorrelationDistribution().size();
				final int sizeb = cellType.getOtherCellTypesCorrelationDistribution().size();
				final float sqrt = Double.valueOf(Math.sqrt((sizea * sizeb * 1d) / (1d * (sizea + sizeb))))
						.floatValue();
				final float dStatistic = supremum * sqrt;
				final double ksPvalue = ksPValue(supremum, sizea, sizeb);
				if (!permutatedData) {
					cellType.setSizeA(sizea);
					cellType.setSizeB(sizeb);
					cellType.setEnrichment(supremum, dStatistic, supremumX, 1d * supremumX / singleCellList.size());
					cellType.setKSTestPvalue(ksPvalue);

					final String ksSignificancyString = "";
//					final String[] significancies = { "*", "**", "***" };
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
						for (int i = 0; i <= secondaryEnrichmentIndex; i++) {
							final SingleCell singleCell = singleCellList.get(i);

							float a = 0.0f;
							float b = 0.0f;

							if (cellTypeName.equals(singleCell.getCellType(this.cellTypeBranch))) {
								// this is the difference with the unweigthed, using the correlation, instead of
								// just counting
								numeratorA += Math.abs(singleCell.getCorrelation());
								a = numeratorA / denominatorA;
								b = previousB;
							} else {
								numeratorB += Math.abs(singleCell.getCorrelation());
								a = previousA;
								b = numeratorB / denominatorB;
							}
							// we force to look only to positive ones
							if (a > b) {
								final float difference = a - b;
								if (Math.abs(difference) > Math.abs(secondarySupremum)) {
									secondarySupremum = difference;
									secondarySupremumX = i + 1;
									if (generateCharts) {
										if (secondarySupremumLineSeries == null) {
											secondarySupremumLineSeries = new XYSeries("secondary supremum");
										}
										secondarySupremumLineSeries.clear();
										secondarySupremumLineSeries.add(i + 1, a);
										secondarySupremumLineSeries.add(i + 1, b);
									}
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
			// do not generate plot for cell types with a minimum number of cells
			if (cellsOfType.size() < minCellsPerCellTypeForPDF) {
				continue;
			}
			// only output charts of positive correlations or that have secondary positive
			// enrichment scores
			if (!plotNegativeEnrichedCellTypes && cellType.getEnrichmentScore() < 0f) {
				continue;
			}
			if (generateCharts && !permutatedData && minCellsPerCellTypeForPDF <= cellsOfType.size()) {

				final String title = "Enrichment Score calculation for cell type: '" + cellTypeName + "'";

				String subtitle = "(ews: " + format.format(cellType.getEnrichmentScore()) + ", supX: "
						+ cellType.getSupremumX();
				if (cellType.getSecondaryEnrichmentScore() != null) {
					subtitle += ", 2nd ews: " + format.format(cellType.getSecondaryEnrichmentScore());
					subtitle += ", 2nd sup size: " + format.format(cellType.getSecondaryEnrichmentScore());
					subtitle += ", 2nd supX: " + cellType.getSecondarySupremumX();
				}
				subtitle += ", sizes: " + cellType.getCellTypeCorrelationDistribution().size() + " vs "
						+ cellType.getOtherCellTypesCorrelationDistribution().size() + ")";
				final JFreeChart chartScoreCalculation = createScoreCalculationChart(title, singleCellList.size(),
						scoreSeriesType, scoreSeriesOtherType, supremumLineSeries, secondarySupremumLineSeries);
				chartScoreCalculation.addSubtitle(new TextTitle(subtitle));
				cellType.setEnrichmentScoreCalculationLineChart(chartScoreCalculation);

				// and two series for correlation chart
				final JFreeChart chart = createCorrelationDistributionChart(cellTypeName, corrFrequencyType,
						corrFrequencyOthers);
				cellType.setCorrelationDistributionChart(chart);

				// chart with the histogram of number of genes per cell in the cell type
				final JFreeChart chart2 = createHistogramOfCorrelatingGenesChart(cellTypeName, histogramOfNumGenes);
				cellType.setHistogramOfCorrelatingGenesChart(chart2);
			}

		}
	}

	/**
	 * When product of sample sizes is less than this value, 2-sample K-S test is
	 * exact
	 */
	private static final int SMALL_SAMPLE_PRODUCT = 200;

	/**
	 * When product of sample sizes exceeds this value, 2-sample K-S test uses
	 * asymptotic distribution for strict inequality p-value.
	 */
	private static final int LARGE_SAMPLE_PRODUCT = 10000;

	/**
	 * Default number of iterations used by
	 * {@link #monteCarloP(double, int, int, boolean, int)}
	 */
	private static final int MONTE_CARLO_ITERATIONS = 1000000;

	private double ksPValue(double dStatistic, int a, int b) {

		final long lengthProduct = (long) a * b;
		final boolean strict = true; // checks whether the supremum of random permutations of data are STRICTLY
										// superior to that dStatistic
		if (lengthProduct < SMALL_SAMPLE_PRODUCT) {
			return test.exactP(dStatistic, a, b, strict);
		}
		if (lengthProduct < LARGE_SAMPLE_PRODUCT) {
			return test.monteCarloP(dStatistic, a, b, strict, MONTE_CARLO_ITERATIONS);
		}
		return test.approximateP(dStatistic, a, b);
	}

	private int lookForSecondaryEnrichmentIndex(int supremumX, TDoubleList differences) {

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

	private JFreeChart createHistogramOfCorrelatingGenesChart(String cellTypeName, TIntIntMap histogramOfNumGenes) {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		final TIntList keys = new TIntArrayList(histogramOfNumGenes.keys());
		keys.sort();
		int totalCells = 0;
		for (final int numGenes : keys.toArray()) {
			final int frequency = histogramOfNumGenes.get(numGenes);
			totalCells += frequency;
			dataset.addValue(frequency, "# genes", String.valueOf(numGenes));
		}
		for (int i = 0; i < keys.size(); i++) {
			int accumulativeNumGenes = 0;
			for (int j = i; j < keys.size(); j++) {
				accumulativeNumGenes += histogramOfNumGenes.get(keys.get(j));

			}
			dataset.addValue(accumulativeNumGenes, "# genes or more", String.valueOf(keys.get(i)));
		}
		String title = "Distribution of # of genes correlating in cell type '" + cellTypeName + "'";

		title += " (" + totalCells + ")";
		final JFreeChart chart = ChartFactory.createBarChart(title, "# of genes correlating", "# cells", dataset,
				PlotOrientation.VERTICAL, true, false, false);
		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setDefaultItemLabelGenerator(new IntegerCategoryItemLabelGenerator());
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
		renderer.setItemMargin(0.1);
		renderer.setBarPainter(new StandardBarPainter());
		return chart;
	}

	private JFreeChart createScoreCalculationChart(String chartTitle, int size, XYSeries scoreSeriesType,
			XYSeries scoreSeriesOtherType, XYSeries supremumLineSeries, XYSeries secondarySupremumLineSeries) {
		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(scoreSeriesType);
		dataset.addSeries(scoreSeriesOtherType);
		dataset.addSeries(supremumLineSeries);
		if (secondarySupremumLineSeries != null) {
			dataset.addSeries(secondarySupremumLineSeries);
		}

		final JFreeChart chart = ChartFactory.createXYLineChart(chartTitle, "cell #", "Cumulative Probability [Fn(X)]",
				dataset);
		final XYPlot plot = (XYPlot) chart.getPlot();
		plot.getDomainAxis().setLowerBound(1.0);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setSeriesRenderingOrder(SeriesRenderingOrder.REVERSE);
		final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setSeriesPaint(2, Color.black);
		if (secondarySupremumLineSeries != null) {
			renderer.setSeriesPaint(3, Color.MAGENTA);
		}
		return chart;
	}

	private JFreeChart createCorrelationDistributionChart(String cellTypeName, TDoubleList correlationsType,
			TDoubleList correlationsOthers) {
		// create chart
		final XYSeriesCollection histogramDataset = new XYSeriesCollection();

		final XYSeries seriesType = new XYSeries(cellTypeName);
		histogramDataset.addSeries(seriesType);
		final int numBins = Math.min(
				Histogram.getSturgisRuleForHistogramBins(Math.max(correlationsType.size(), correlationsOthers.size())),
				10);

		final double max = 1.0;
		final double min = -1.0;
		double[][] binStats = Histogram.calcHistogram(correlationsType.toArray(), min, max, numBins);
		for (int i = 0; i < binStats[0].length; i++) {
			final double bin = binStats[2][i];
			final double lowerBound = binStats[0][i];
			final double upperBound = binStats[1][i];
			final double x = (upperBound - lowerBound) / 2.0 + lowerBound;
			seriesType.add(x, bin);
		}
		final XYSeries seriesOhersType = new XYSeries("Others");
		// TODO
		// no others as requested by Casimir
//		histogramDataset.addSeries(seriesOhersType);
		binStats = Histogram.calcHistogram(correlationsOthers.toArray(), min, max, numBins);
		for (int i = 0; i < binStats[0].length; i++) {
			final double bin = binStats[2][i];
			final double lowerBound = binStats[0][i];
			final double upperBound = binStats[1][i];
			final double x = (upperBound - lowerBound) / 2.0 + lowerBound;
			seriesOhersType.add(x, bin);
		}

		final String plotTitle = "'" + cellTypeName + "' (sizes: " + correlationsType.size() + " vs "
				+ correlationsOthers.size() + ")";
		final String xaxis = "Pearson's correlation";
		final String yaxis = "Frequency (# of cells)";
		final PlotOrientation orientation = PlotOrientation.VERTICAL;
		final boolean show = true;
		final boolean toolTips = false;
		final boolean urls = false;

		final JFreeChart chart = ChartFactory.createXYLineChart(plotTitle, xaxis, yaxis, histogramDataset, orientation,
				show, toolTips, urls);
		chart.getPlot().setBackgroundPaint(Color.lightGray);
		return chart;

	}

}
