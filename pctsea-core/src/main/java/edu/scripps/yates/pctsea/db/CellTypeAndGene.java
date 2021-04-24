package edu.scripps.yates.pctsea.db;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class CellTypeAndGene implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8784588919727820591L;
	@Id
	private String id;
	@Indexed
	private String datasetTag;
	@Indexed
	private String gene;
	@Indexed
	private String cellType;
	private long cellCount;

	public CellTypeAndGene() {

	}

	public CellTypeAndGene(String id, String datasetTag, String gene, String cellType, long cellCount) {
		this.id = id;
		this.datasetTag = datasetTag;
		this.gene = gene;
		this.cellType = cellType;
		this.cellCount = cellCount;
	}

	public String getId() {
		return id;
	}

	public String getDatasetTag() {
		return datasetTag;
	}

	public String getGene() {
		return gene;
	}

	public long getCellCount() {
		return cellCount;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setDatasetTag(String datasetTag) {
		this.datasetTag = datasetTag;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	public void setCellCount(long cellCount) {
		this.cellCount = cellCount;
	}

	public String getCellType() {
		return cellType;
	}

	public void setCellType(String cellType) {
		this.cellType = cellType;
	}

}
