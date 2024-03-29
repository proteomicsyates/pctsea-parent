package edu.scripps.yates.pctsea.utils;

import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.pctsea.model.ScoringSchema;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.email.EmailSender;
import edu.scripps.yates.utilities.swing.StatusListener;

public class EmailUtil {
	private final static Logger log = Logger.getLogger(EmailUtil.class);

	public static String sendEmailWithResults(PCTSEAResult result, String fromEmail,
			StatusListener<Boolean> statusListener) {
		final InputParameters inputParameters = result.getRunLog().getInputParameters();
		statusListener.onStatusUpdate("Sending email with results to " + inputParameters.getEmail());
		// SUBJECT
		final String subject = "pCtSEA results '" + result.getRunLog().getTimeStamp() + " - "
				+ inputParameters.getOutputPrefix();

		// BODY
		final StringBuilder body = new StringBuilder();
		body.append("<h4>This email is generated from pCtSEA tool</h4><br>");
		body.append("Run submited: " + result.getRunLog().getStarted() + " <br> ");
		body.append("Run finished: " + result.getRunLog().getFinished() + " <br> ");
		body.append("Running time: " + DatesUtil.getDescriptiveTimeFromMillisecs(result.getRunLog().getRunningTime())
				+ " <br>");
		body.append("Size of input protein list: " + result.getRunLog().getNumInputGenes() + "<br>");
		final List<String> inputGenesNotFound = result.getRunLog().getInputGenesNotFound();
		body.append("Number of input proteins not mapped: " + inputGenesNotFound.size() + "<br>");

		if (!inputGenesNotFound.isEmpty()) {
			body.append("Proteins not mapped: ");
			final StringBuilder sb = new StringBuilder();
			for (final String inputGeneNotFound : inputGenesNotFound) {
				if (!"".equals(sb.toString())) {
					sb.append(",");
				}
				sb.append(inputGeneNotFound);
			}
			body.append(sb.toString());
		}
		body.append("<br><br>");
		// parameters
		body.append("Parameters: <br> ");
		body.append("<ul>");
		body.append("<li>" + InputParameters.EEF + ": " + FilenameUtils.getName(inputParameters.getInputDataFile())
				+ " </li> ");

		body.append("<li>" + InputParameters.DATASETS + ":<ul>");
		if (inputParameters.getDatasets() != null) {
			for (final Dataset dataset : inputParameters.getDatasets()) {
				body.append("<li>" + dataset.getTag() + " (" + dataset.getName() + " - " + dataset.getReference() + ")"
						+ "</li>");
			}
		} else {
			body.append("<li>All</li>");
		}
		body.append("</ul>");

		body.append("<li>" + InputParameters.OUT + ": " + inputParameters.getOutputPrefix() + "</li>");
		body.append("<li>" + InputParameters.PERM + ": " + inputParameters.getNumPermutations() + "</li>");
		body.append("<li>" + InputParameters.PLOT_NEGATIVE_ENRICHED + ": " + inputParameters.isPlotNegativeEnriched()
				+ "</li>");
		body.append("<li>" + InputParameters.LOAD_RANDOM + ": " + inputParameters.isLoadRandom() + "</li>");
		body.append("<li>" + InputParameters.WRITE_SCORES + ": " + inputParameters.isWriteScoresFile() + "</li>");
		body.append("<li>" + InputParameters.CREATE_ZIP_FILE + ": " + inputParameters.isCreateZipFile() + "</li>");
//		body.append("<li>" + InputParameters.UNIPROT_RELEASE + ": " + inputParameters.getUniprotRelease() + "</li>");
		body.append("<li>" + InputParameters.CELL_TYPES_CLASSIFICATION + ": " + inputParameters.getCellTypeBranch()
				+ "</li>");
		body.append("<li>" + InputParameters.MINIMUM_CORRELATION + ": " + inputParameters.getMinCorr() + "</li>");
		int round = 1;
		for (final ScoringSchema scoringSchema : inputParameters.getScoringSchemas()) {
			if (inputParameters.getScoringSchemas().size() > 1) {
				body.append("<li>Round " + round + ":<ul>");
			}
			body.append("<li>" + InputParameters.SCORING_METHOD + ": " + scoringSchema.getScoringMethod().getScoreName()
					+ "</li>");
			body.append("<li>" + InputParameters.MIN_SCORE + ": "
					+ scoringSchema.getScoringThreshold().getThresholdValue() + "</li>");
			body.append("<li>" + InputParameters.MIN_GENES_CELLS + ": " + scoringSchema.getMinGenesCells() + "</li>");
			body.append("</ul></li>");
			round++;
		}
		body.append("</ul>");
		body.append("<br><br>");
		// results
		final List<CellTypeClassification> cellTypes = result.getSignificantTypes();
		if (cellTypes == null || cellTypes.isEmpty()) {
			body.append("There is no significantly enriched cell types in your data<br>");
		} else {
			String plural = "is";
			String plural2 = "";
			if (cellTypes.size() > 1) {
				plural = "are";
				plural2 = "s";
			}
			body.append("There " + plural + " " + cellTypes.size() + " significantly enriched cell type" + plural2
					+ " in your data:<br>");
			body.append("<ul>");
			for (final CellTypeClassification cellType : cellTypes) {
				body.append("<li>" + cellType.getName() + "</li>");
			}
			body.append("</ul>");
		}
		if (result.getResultsFiles().size() == 1) {
			body.append("<br>You can download and visualize the results at this URL: <a href=\""
					+ result.getUrlToViewers().get(0) + "\">" + result.getUrlToViewers().get(0) + "</a><br>");
			body.append("<br><br><br>");
		} else {
			body.append(
					"Your can download and visualize the results in these 2 URLs, corresponding to the 2 rounds approach:<br>");
			body.append("First round: <a href=\"" + result.getUrlToViewers().get(0) + "\">"
					+ result.getUrlToViewers().get(0) + "</a><br>");
			body.append("Second round: <a href=\"" + result.getUrlToViewers().get(1) + "\">"
					+ result.getUrlToViewers().get(1) + "</a><br>");
		}
		body.append(
				"Please, don't hesitate to contact <a href=\"mailto:salvador@scripps.edu\">salvador@scripps.edu</a> for more information about pCtSEA");

		// DESTINATION EMAIL
		final String destinationEmail = inputParameters.getEmail();

		// FROM EMAIL

		// SEND THE EMAIL
		final String error = EmailSender.sendEmail(subject, body.toString(), fromEmail, destinationEmail, fromEmail,
				true);
		if (error != null) {
			statusListener
					.onStatusUpdate("Error sending email. Perhaps emails cannot be sent from this machine: " + error);
		} else {
			statusListener.onStatusUpdate("Email sent!");
		}
		return error;
	}
}
