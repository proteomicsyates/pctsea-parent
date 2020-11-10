package edu.scripps.yates.pctsea.model.charts;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.XYSeries;

public class LabeledXYSeries extends XYSeries {
	private final List<String> labels = new ArrayList<String>();

	public LabeledXYSeries(Comparable key) {
		super(key);
	}

	public void add(String label, double x, double y) {
		labels.add(label);
		super.add(x, y);
	}

	public String getLabel(int itemIndex) {
		return labels.get(itemIndex);
	}

}
