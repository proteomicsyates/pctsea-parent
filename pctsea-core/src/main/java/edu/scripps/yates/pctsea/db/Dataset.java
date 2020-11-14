package edu.scripps.yates.pctsea.db;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class Dataset implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5217284496561243554L;
	@Id
	private String id;
	@Indexed(unique = true)
	private String tag;
	@Indexed(unique = true)
	private String name;
	private String reference;

	public Dataset() {

	}

	public Dataset(String tag, String name, String reference) {
		super();
		this.tag = tag;
		this.name = name;
		this.reference = reference;
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
