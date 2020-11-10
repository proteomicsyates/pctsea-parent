package edu.scripps.yates.pctsea.model.charts;

import java.text.DecimalFormat;

import org.jfree.chart.labels.AbstractCategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.data.category.CategoryDataset;

public class DoubleCategoryItemLabelGenerator extends AbstractCategoryItemLabelGenerator
		implements CategoryItemLabelGenerator {

	/**
	* 
	*/
	private static final long serialVersionUID = -331695488937565934L;
	private final String pattern;

	public DoubleCategoryItemLabelGenerator(String pattern) {
		super("{2}", new DecimalFormat(pattern));
		this.pattern = pattern;
	}

	@Override
	public String generateLabel(CategoryDataset dataset, int row, int column) {
		return new DecimalFormat(pattern).format(dataset.getValue(row, column));
	}

}