package edu.scripps.yates.pctsea.model.charts;

import java.text.NumberFormat;

import org.jfree.chart.labels.AbstractCategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.data.category.CategoryDataset;

public class IntegerCategoryItemLabelGenerator extends AbstractCategoryItemLabelGenerator
		implements CategoryItemLabelGenerator {

	/**
	* 
	*/
	private static final long serialVersionUID = -331695488937565934L;

	public IntegerCategoryItemLabelGenerator() {
		super("{2}", NumberFormat.getIntegerInstance());

	}

	@Override
	public String generateLabel(CategoryDataset dataset, int row, int column) {
		return String.valueOf(dataset.getValue(row, column).intValue());
	}

}