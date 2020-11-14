package edu.scripps.yates.pctsea.model;

import java.io.File;
import java.net.URL;

import edu.scripps.yates.pctsea.db.PctseaRunLog;

/**
 * Represents the final file results and the URL to redirect the user to to
 * visualize the results
 * 
 * @author salvador
 *
 */
public class PCTSEAResult {
	private File resultsFile;
	private File resultsFileForViewer;
	private URL urlToViewer;
	private PctseaRunLog runLog;

	public PCTSEAResult() {

	}

	public PCTSEAResult(File resultsFile, File resultsFileForViewer, URL urlToViewer, PctseaRunLog runLog) {
		this.resultsFile = resultsFile;
		this.resultsFileForViewer = resultsFileForViewer;
		this.urlToViewer = urlToViewer;
		this.runLog = runLog;
	}

	public File getResultsFile() {
		return resultsFile;
	}

	public URL getUrlToViewer() {
		return urlToViewer;
	}

	public PctseaRunLog getRunLog() {
		return runLog;
	}

	public File getResultsFileForViewer() {
		return resultsFileForViewer;
	}

	public void setResultsFile(File resultsFile) {
		this.resultsFile = resultsFile;
	}

	public void setResultsFileForViewer(File resultsFileForViewer) {
		this.resultsFileForViewer = resultsFileForViewer;
	}

	public void setUrlToViewer(URL urlToViewer) {
		this.urlToViewer = urlToViewer;
	}

	public void setRunLog(PctseaRunLog runLog) {
		this.runLog = runLog;
	}
}
