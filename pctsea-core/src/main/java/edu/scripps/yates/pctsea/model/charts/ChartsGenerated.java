package edu.scripps.yates.pctsea.model.charts;

import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis.utils.ByteArrayOutputStream;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

import edu.scripps.yates.pctsea.utils.PDFUtils;

public class ChartsGenerated extends ArrayList<ByteArrayOutputStream> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5214760670709745486L;
	private static ChartsGenerated instance;
	public static int scaleFactor = 4;// to increase resolution;
	private final List<File> pdfFiles = new ArrayList<File>();

	public static ChartsGenerated getNewInstance() {
		instance = null;
		return getInstance();
	}

	public static ChartsGenerated getInstance() {
		if (instance == null) {
			instance = new ChartsGenerated();
		}
		return instance;
	}

	public void saveChartsAsPDF() throws IOException {
		for (int i = 0; i < this.size(); i++) {
			final byte[][] byteArrays = new byte[1][];
			final ByteArrayOutputStream byteArrayOutput = this.get(i);
			final File outputPDFFile = this.pdfFiles.get(i);
			byteArrays[0] = byteArrayOutput.toByteArray();
			PDFUtils.createPDF(outputPDFFile, 1.0 / scaleFactor, byteArrays);
		}
	}

//	private File getOutputPDFFile(File folder, String pdfName, String prefix) {
//		return new File(folder.getAbsolutePath() + File.separator + prefix + "_" + pdfName + ".pdf");
//	}

	public void saveScaledChartAsPNGInMemory(JFreeChart chart, int width, int height, boolean addToFirstPosition,
			File pdfFile) throws FileNotFoundException, IOException {
		commonFormatting(chart);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		ChartUtils.writeScaledChartAsPNG(out, chart, width, height, scaleFactor, scaleFactor);
		if (addToFirstPosition) {
			this.add(0, out);
			this.pdfFiles.add(0, pdfFile);
		} else {
			this.add(out);
			this.pdfFiles.add(pdfFile);
		}
	}

	/**
	 * Common formatting as title fonts
	 * 
	 * @param chart
	 */
	private void commonFormatting(JFreeChart chart) {
		chart.getTitle().setFont(new Font("Tahoma", Font.PLAIN, 13));
		if (chart.getSubtitleCount() > 0) {
			for (int i = 0; i < chart.getSubtitleCount(); i++) {
				final Title title = chart.getSubtitle(i);
				if (title instanceof TextTitle) {
					((TextTitle) title).setFont(new Font("Tahoma", Font.PLAIN, 10));
				}
			}
		}
	}
}
