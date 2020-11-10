package edu.scripps.yates.pctsea.model.charts;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.AbstractXYDataset;

public class LabeledXYDataset extends AbstractXYDataset {

	/**
	 * 
	 */
	private static final long serialVersionUID = 996152950207090568L;
	private final List<LabeledXYSeries> seriesList = new ArrayList<LabeledXYSeries>();

	public void add(Comparable seriesKey, double x, double y, String label) {
		LabeledXYSeries series = null;
		for (final LabeledXYSeries labeledXYSeries : this.seriesList) {
			if (labeledXYSeries.getKey().compareTo(seriesKey) == 0) {
				series = labeledXYSeries;
			}
		}
		if (series == null) {
			series = new LabeledXYSeries(seriesKey);
			this.seriesList.add(series);
		}
		series.add(label, x, y);
	}

	public String getLabel(int seriesIndex, int itemIndex) {
		return this.seriesList.get(seriesIndex).getLabel(itemIndex);
	}

	@Override
	public int getItemCount(int seriesIndex) {
		return this.seriesList.get(seriesIndex).getItemCount();
	}

	@Override
	public Number getX(int seriesIndex, int itemIndex) {
		return this.seriesList.get(seriesIndex).getX(itemIndex);
	}

	@Override
	public Number getY(int seriesIndex, int itemIndex) {
		return this.seriesList.get(seriesIndex).getY(itemIndex);
	}

	@Override
	public int getSeriesCount() {
		return this.seriesList.size();
	}

	@Override
	public Comparable getSeriesKey(int seriesIndex) {
		return this.seriesList.get(seriesIndex).getKey();
	}

}
