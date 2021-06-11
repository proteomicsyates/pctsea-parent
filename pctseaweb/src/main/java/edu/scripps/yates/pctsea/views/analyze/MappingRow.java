package edu.scripps.yates.pctsea.views.analyze;

import java.io.Serializable;
import java.util.Map;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

public class MappingRow implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1233754573737399023L;
	private String inputProteinGene;
	private double inputExpression;
	private Map<String, String> mappedGenesByDatasetByInput = new THashMap<String, String>();
	private TObjectLongMap<String> numberCellsByDatasetByInput = new TObjectLongHashMap<String>();

	public MappingRow() {

	}

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

	public Map<String, String> getMappedGenesByDatasetByInput() {
		return mappedGenesByDatasetByInput;
	}

	public void setMappedGenesByDatasetByInput(Map<String, String> mappedGenesByDatasetByInput) {
		this.mappedGenesByDatasetByInput = mappedGenesByDatasetByInput;
	}

	public TObjectLongMap<String> getNumberCellsByDatasetByInput() {
		return numberCellsByDatasetByInput;
	}

	public void setNumberCellsByDatasetByInput(TObjectLongMap<String> numberCellsByDatasetByInput) {
		this.numberCellsByDatasetByInput = numberCellsByDatasetByInput;
	}

	public void setInputProteinGene(String inputProteinGene) {
		this.inputProteinGene = inputProteinGene;
	}

	public void setInputExpression(double inputExpression) {
		this.inputExpression = inputExpression;
	}
}
