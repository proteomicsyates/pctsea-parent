package edu.scripps.yates.pctsea.model;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.pctsea.db.PctseaRunLog;

/**
 * Represents the final file results and the URL to redirect the user to to
 * visualize the results
 * 
 * @author salvador
 *
 */
public class PCTSEAResult {
	private final List<File> resultsFiles = new ArrayList<File>();
	private final List<URL> urlToViewers = new ArrayList<URL>();
	private PctseaRunLog runLog;
	private List<CellTypeClassification> significantTypes;

	public PCTSEAResult() {

	}

	public PCTSEAResult(PctseaRunLog runLog) {

		this.runLog = runLog;
	}

	public PCTSEAResult(File resultsFile, URL urlToViewer, PctseaRunLog runLog) {
		resultsFiles.add(resultsFile);
		urlToViewers.add(urlToViewer);
		this.runLog = runLog;
	}

	public List<File> getResultsFiles() {
		return resultsFiles;
	}

	public List<URL> getUrlToViewers() {
		return urlToViewers;
	}

	public PctseaRunLog getRunLog() {
		return runLog;
	}

	public void addResultsFile(File resultsFile) {
		resultsFiles.add(resultsFile);
	}

	public void addUrlToViewer(URL urlToViewer) {
		urlToViewers.add(urlToViewer);
	}

	public void setRunLog(PctseaRunLog runLog) {
		this.runLog = runLog;
	}

	public List<CellTypeClassification> getSignificantTypes() {
		return significantTypes;
	}

	public void setSignificantTypes(List<CellTypeClassification> significantTypes) {
		this.significantTypes = significantTypes;
	}

	public List<CellTypeClassification> getSignificantCellTypes() {
		return significantTypes;
	}
}
