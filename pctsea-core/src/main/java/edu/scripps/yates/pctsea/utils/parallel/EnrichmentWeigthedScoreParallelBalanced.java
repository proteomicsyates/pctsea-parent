package edu.scripps.yates.pctsea.utils.parallel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.logging.LogLevel;

import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.model.ScoringMethod;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.utilities.pi.ParIterator;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class EnrichmentWeigthedScoreParallelBalanced extends EnrichmentWeigthedScoreParallel {

	public EnrichmentWeigthedScoreParallelBalanced(ParIterator<CellTypeClassification> iterator, int numCore,
			List<SingleCell> singleCellList, boolean permutatedData, boolean plotNegativeEnrichedCellTypes,
			String scoreName, File resultsSubfolderForCellTypes, String prefix, boolean compensateWithNegativeSupremum,
			ScoringMethod scoringMethod) {
		super(iterator, numCore, singleCellList, permutatedData, plotNegativeEnrichedCellTypes, scoreName,
				resultsSubfolderForCellTypes, prefix, compensateWithNegativeSupremum, scoringMethod);
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
				final int posX = i + 1;

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
					final float normalizedSupremumX = 1f * supremumX / singleCellList.size();
					if (!compensateWithNegativeSupremum) {
						cellType.setEnrichment(supremum * (1 - normalizedSupremumX), dStatistic, supremumX,
								normalizedSupremumX);
					} else {
						final Pair<Integer, Float> priorNegativeSupremum = lookForPriorNegativeSupremum(supremumX,
								differences);
						if (priorNegativeSupremum != null) {
							supremum = supremum - Math.abs(priorNegativeSupremum.getSecondElement());

						}
						cellType.setEnrichment(supremum * (1 - normalizedSupremumX), dStatistic, supremumX,
								normalizedSupremumX);

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
					final float normalizedSupremumX = 1f * supremumX / singleCellList.size();
					cellType.addRandomEnrichment(supremum * normalizedSupremumX, dStatistic);
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

}
