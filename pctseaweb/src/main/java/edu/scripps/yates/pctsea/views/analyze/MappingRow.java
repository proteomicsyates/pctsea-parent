package edu.scripps.yates.pctsea.views.analyze;

import java.util.Map;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

public class MappingRow {
	private final String inputProteinGene;
	private final double inputExpression;
	private final Map<String, String> mappedGenesByDatasetByInput = new THashMap<String, String>();
	private final TObjectLongMap<String> numberCellsByDatasetByInput = new TObjectLongHashMap<String>();

	public MappingRow(String inputProteinGene, double inputExpression) {
		this.inputProteinGene = inputProteinGene;
		this.inputExpression = inputExpression;
	}

	public String getInputProteinGene() {
		return inputProteinGene;
	}

	public double getInputExpression() {
		return inputExpression;
	}

	public String getMappedGene(String dataset) {
		return mappedGenesByDatasetByInput.get(dataset);
	}

	public long getNumberCellsByDataset(String dataset) {
		return numberCellsByDatasetByInput.get(dataset);
	}

	public void addMapping(String dataset, String mappedGene, long numberCells) {
		mappedGenesByDatasetByInput.put(dataset, mappedGene);
		numberCellsByDatasetByInput.put(dataset, numberCells);
	}
}
