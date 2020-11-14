package edu.scripps.yates.pctsea.db;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class SingleCell implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8889064067175022963L;
	@Id
	private String id;
	@Indexed(unique = true)
	private String name;
	private String type;
	private String biomaterial;
	private String datasetTag;

	public SingleCell() {

	}

	public SingleCell(String name, String type, String biomaterial, String datasetTag) {
		this.name = name;
		this.type = type;
		this.biomaterial = biomaterial;
		this.datasetTag = datasetTag;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getBiomaterial() {
		return biomaterial;
	}

	public void setBiomaterial(String biomaterial) {
		this.biomaterial = biomaterial;
	}

	public String getDatasetTag() {
		return datasetTag;
	}

	public void setDatasetTag(String datasetTag) {
		this.datasetTag = datasetTag;
	}
}
