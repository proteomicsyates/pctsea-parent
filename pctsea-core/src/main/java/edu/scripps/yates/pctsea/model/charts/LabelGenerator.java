package edu.scripps.yates.pctsea.model.charts;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

public class LabelGenerator implements XYItemLabelGenerator {

	@Override
	public String generateLabel(XYDataset dataset, int series, int item) {
		final LabeledXYDataset labeledDataset = (LabeledXYDataset) dataset;
		final String label = labeledDataset.getLabel(series, item);
		return label;
	}

}
