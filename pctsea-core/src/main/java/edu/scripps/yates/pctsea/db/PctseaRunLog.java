package edu.scripps.yates.pctsea.db;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;

import edu.scripps.yates.pctsea.model.InputParameters;

public class PctseaRunLog {
	@Id
	private String id;
	private Date started;
	private Date finished;
	private String timeStamp;
	private int numInputGenes;
	private InputParameters inputParameters;
	private List<String> inputGenesNotFound;
	private int numCellsPassingScoreThreshold;

	public PctseaRunLog() {

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getStarted() {
		return started;
	}

	public void setStarted(Date started) {
		this.started = started;
	}

	public long getRunningTime() {
		if (finished != null && started != null) {
			return finished.toInstant().toEpochMilli() - started.toInstant().toEpochMilli();
		} else {
			return 0l;
		}
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public int getNumInputGenes() {
		return numInputGenes;
	}

	public void setNumInputGenes(int numInputGenes) {
		this.numInputGenes = numInputGenes;
	}

	public void setInputGenesNotFound(List<String> inputGenesNotFound) {
		this.inputGenesNotFound = inputGenesNotFound;
	}

	public void setInputParameters(InputParameters inputParameters) {
		this.inputParameters = inputParameters;
	}

	public InputParameters getInputParameters() {
		return inputParameters;
	}

	public Date getFinished() {
		return finished;
	}

	public void setFinished(Date finished) {
		this.finished = finished;
	}

	public List<String> getInputGenesNotFound() {
		return inputGenesNotFound;
	}

	public int getNumCellsPassingScoreThreshold() {
		return numCellsPassingScoreThreshold;
	}

	public void setNumCellsPassingScoreThreshold(int numCellsPassingScoreThreshold) {
		this.numCellsPassingScoreThreshold = numCellsPassingScoreThreshold;
	}
}
