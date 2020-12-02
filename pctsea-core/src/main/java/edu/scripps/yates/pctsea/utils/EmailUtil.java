package edu.scripps.yates.pctsea.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.email.EmailSender;
import edu.scripps.yates.utilities.strings.StringUtils;

public class EmailUtil {
	private final static Logger log = Logger.getLogger(EmailUtil.class);

	public static String sendEmailWithResults(PCTSEAResult result) {
		final InputParameters inputParameters = result.getRunLog().getInputParameters();
		log.info("Sending email with results to " + inputParameters.getEmail());
		// SUBJECT
		final String subject = "PCTSEA results '" + result.getRunLog().getTimeStamp() + " - "
				+ inputParameters.getOutputPrefix();

		// BODY
		final StringBuilder body = new StringBuilder();
		body.append("This email is generated from PCTSEA tool\n");
		body.append("Run submited: " + result.getRunLog().getStarted() + "\n");
		body.append("Run finished: " + result.getRunLog().getFinished() + "\n");
		body.append("Running time: " + DatesUtil.getDescriptiveTimeFromMillisecs(result.getRunLog().getRunningTime())
				+ "\n");

		// parameters
		body.append("Parameters:\n");
		body.append(InputParameters.EEF + ": " + FilenameUtils.getName(inputParameters.getInputDataFile()) + "\n");
		body.append(InputParameters.MIN_CELLS_PER_CELL_TYPE + ": " + inputParameters.getMinCellsPerCellType() + "\n");
		body.append(InputParameters.MIN_CORRELATION + ": " + inputParameters.getMinCorrelation() + "\n");
		body.append(InputParameters.MIN_GENES_CELLS + ": " + inputParameters.getMinGenesCells() + "\n");
		if (inputParameters.getDatasets() != null) {
			body.append(InputParameters.DATASETS + ": "
					+ StringUtils.getSortedSeparatedValueStringFromChars(inputParameters.getDatasets(), ",") + "\n");
		} else {
			body.append(InputParameters.DATASETS + ": ALL");
		}
		body.append(InputParameters.OUT + ": " + inputParameters.getOutputPrefix() + "\n");
		body.append(InputParameters.PERM + ": " + inputParameters.getNumPermutations() + "\n");
		body.append(InputParameters.PLOT_NEGATIVE_ENRICHED + ": " + inputParameters.isPlotNegativeEnriched() + "\n");
		body.append(InputParameters.LOAD_RANDOM + ": " + inputParameters.isLoadRandom() + "\n");
		body.append(InputParameters.CHARTS + ": " + inputParameters.isGenerateCharts() + "\n");
		body.append("\n\n");
		// results
		body.append("You can access to your results at this location in the machine you run it: "
				+ result.getResultsFile().getAbsolutePath() + "\n");
		if (result.getUrlToViewer() != null) {
			body.append(
					"Alternatively, you can go to this URL to visualize the results (also to download the results): "
							+ result.getUrlToViewer() + "\n");
			body.append("<a hre=\"" + result.getUrlToViewer() + "\">testing this link: " + result.getUrlToViewer()
					+ "</a>");
			body.append("\n\n\n");
		}
		body.append("Please, don't hesitate to contact 'salvador@scripps.edu' for more information about PCTSEA");

		// DESTINATION EMAIL
		final String destinationEmail = inputParameters.getEmail();

		// FROM EMAIL
		final String fromEmail = "salvador@scripps.edu";

		// SEND THE EMAIL
		final String error = EmailSender.sendEmail(subject, body.toString(), destinationEmail, fromEmail);
		if (error != null) {
			log.error("Error sending email. Perhaps emails cannot be sent from this machine.");
		} else {
			log.info("Email sent!");
		}
		return error;
	}
}