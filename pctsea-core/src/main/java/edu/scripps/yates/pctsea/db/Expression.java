package edu.scripps.yates.pctsea.db;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Expression implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2619000403435276306L;

	@Id
	private String id;

	private String cellName;
	private String cellType;
	private String gene;
	private float expression;
	private String projectTag;

	public Expression() {

	}

	public Expression(SingleCell cell, String gene, float expression, String datasetTag) {
		super();
		this.cellName = cell.getName();
		this.cellType = cell.getType();
		this.gene = gene;
		this.expression = expression;
		this.projectTag = datasetTag;
	}

	public Expression(String cellName, String cellType, String gene, float expression, String datasetTag) {
		super();
		this.cellName = cellName;
		this.setCellType(cellType);
		this.gene = gene;
		this.expression = expression;
		this.projectTag = datasetTag;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public float getExpression() {
		return expression;
	}

	public void setExpression(float expression) {
		this.expression = expression;
	}

	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	@Override
	public String toString() {
		final ObjectMapper mapper = new ObjectMapper();

		String jsonString = "";
		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			jsonString = mapper.writeValueAsString(this);
		} catch (final JsonProcessingException e) {
			e.printStackTrace();
		}

		return jsonString;
	}

	public String getCellName() {
		return cellName;
	}

	public void setCellName(String cellName) {
		this.cellName = cellName;
	}

	public String getProjectTag() {
		return projectTag;
	}

	public void setProjectTag(String datasetTag) {
		this.projectTag = datasetTag;
	}

	public String getCellType() {
		return cellType;
	}

	public void setCellType(String cellType) {
		this.cellType = cellType;
	}

	public void setCell(SingleCell singleCell) {
		setCellName(singleCell.getName());
		setCellType(singleCell.getType());
	}
}
