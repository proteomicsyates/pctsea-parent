package edu.scripps.yates.pctsea.model;

public class GeneOccurrence {
	private final String gene;
	private int occurrence = 0;

	public GeneOccurrence(String gene) {
		this.gene = gene;
	}

	public void incrementOccurrence() {
		this.setOccurrence(this.getOccurrence() + 1);
	}

	public String getGene() {
		return gene;
	}

	public int getOccurrence() {
		return occurrence;
	}

	public void setOccurrence(int occurrence) {
		this.occurrence = occurrence;
	}

}
