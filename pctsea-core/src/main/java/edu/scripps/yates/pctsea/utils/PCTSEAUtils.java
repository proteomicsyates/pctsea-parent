package edu.scripps.yates.pctsea.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;

import edu.scripps.yates.pctsea.model.ScoringMethod;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.pi.ConcurrentUtil;

public class PCTSEAUtils {
	public static String parseGeneName(String geneName) {
		if (geneName.contains("_")) {
			final String[] split = geneName.split("_");
			if (split[1].equalsIgnoreCase("human")) {
				return split[0];
			}
		}
		return geneName;
	}

	public synchronized static double[] toDoubleArray(float[] x) {
		final double[] ret = new double[x.length];
		int i = 0;
		for (final float d : x) {
			ret[i] = d;
			i++;
		}
		return ret;
	}

	public static void sortByScoreDescending(List<SingleCell> singleCellList) {
		Collections.sort(singleCellList, new Comparator<SingleCell>() {

			@Override
			public int compare(SingleCell o1, SingleCell o2) {
				return Double.compare(o2.getScoreForRanking(), o1.getScoreForRanking());
			}
		});

	}

	private static File writeTXTFileForChart(JFreeChart chart, File resultsSubfolder, String prefix, String fileName,
			ScoringMethod scoringMethod) throws IOException {
		final File outputTXTFile = getOutputTXTFile(resultsSubfolder, fileName, prefix, scoringMethod);
		final BufferedWriter fw = new BufferedWriter(new FileWriter(outputTXTFile));

		final Plot plot = chart.getPlot();
		ConcurrentUtil.sleep(1L);
		if (plot instanceof XYPlot) {
			final XYPlot xyplot = (XYPlot) plot;
			final String xAxis = xyplot.getDomainAxis().getLabel();
			final String yAxis = xyplot.getRangeAxis().getLabel();
			final String header = "-\t" + xAxis + "\t" + yAxis;
			fw.write(header + "\n");

			for (int datasetIndex = 0; datasetIndex < xyplot.getDatasetCount(); datasetIndex++) {
				final XYDataset dataset = xyplot.getDataset(datasetIndex);
				for (int seriesIndex = 0; seriesIndex < dataset.getSeriesCount(); seriesIndex++) {

					final Comparable seriesKey = dataset.getSeriesKey(seriesIndex);
					for (int itemIndex = 0; itemIndex < dataset.getItemCount(seriesIndex); itemIndex++) {
						final double x = dataset.getXValue(seriesIndex, itemIndex);
						final double y = dataset.getYValue(seriesIndex, itemIndex);
						fw.write(seriesKey + "\t" + x + "\t" + y + "\n");
					}
				}
			}
		} else if (plot instanceof CategoryPlot) {
			final CategoryPlot categoryPlot = (CategoryPlot) plot;
			final String xAxis = categoryPlot.getDomainAxis().getLabel();
			final String yAxis = categoryPlot.getRangeAxis().getLabel();
			final String header = "-\t" + xAxis + "\t" + yAxis;
			fw.write(header + "\n");
			for (int datasetIndex = 0; datasetIndex < categoryPlot.getDatasetCount(); datasetIndex++) {
				final CategoryDataset dataset = categoryPlot.getDataset(datasetIndex);
				for (int rowIndex = 0; rowIndex < dataset.getRowCount(); rowIndex++) {
					final Comparable rowKey = dataset.getRowKey(rowIndex);
					for (int columnIndex = 0; columnIndex < dataset.getColumnCount(); columnIndex++) {
						final Comparable columnKey = dataset.getColumnKey(columnIndex);
						final Number value = dataset.getValue(rowIndex, columnIndex);
						fw.write(rowKey + "\t" + columnKey + "\t" + value + "\n");
					}
				}
			}
		}
		fw.close();
		return outputTXTFile;
	}

//	public static File getChartPDFFile(File folder, String fileName, String prefix) {
//		String fullfileName = prefix + "_" + fileName + ".pdf";
//		fullfileName = FileUtils.checkInvalidCharacterNameForFileName(fullfileName);
//		return new File(folder.getAbsolutePath() + File.separator + fullfileName);
//	}

	public static File getOutputTXTFile(File folder, String fileName, String prefix, ScoringMethod scoringMethod) {
		final String correctedFileName = FileUtils.checkInvalidCharacterNameForFileName(
				prefix + "_" + scoringMethod.getScoreName() + "_" + fileName + ".txt");
		final File file = new File(folder.getAbsolutePath() + File.separator + correctedFileName);
		return file;
	}
}
