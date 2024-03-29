package edu.scripps.yates.pctsea.views.about;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.PctseaRunLog;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.model.ScoringSchema;
import edu.scripps.yates.pctsea.util.PCTSEAConfigurationException;
import edu.scripps.yates.pctsea.util.PCTSEALocalConfiguration;
import edu.scripps.yates.pctsea.views.main.MainView;
import edu.scripps.yates.utilities.dates.DatesUtil;

@Route(value = "about", layout = MainView.class)
@PageTitle("About")
@CssImport("./styles/views/about/about-view.css")
public class AboutView extends Div {
	@Autowired
	private PctseaRunLogRepository runRepo;

	public AboutView() {
	}

	@PostConstruct
	public void init() {
		setId("about-view");
		add(new Label("Proteomics Cell Type Set Enrichment Analysis (PCTSEA)"));

		final Image logo = new Image("images/yateslab.png", "pCtSEA Proteomics Cell Type Set Enrichment Analysis");
//		logo.setHeight("225px");
//		logo.setWidth("225px");

		final String labURL = "http://www.scripps.edu/yates/";
		final Anchor lablink = new Anchor(labURL, "John Yates's laboratory");
		lablink.setTarget("_blank");

		final String scrippsURL = "http://www.scripps.edu/";
		final Anchor scrippsLink = new Anchor(scrippsURL, "Scripps Research Institute");
		scrippsLink.setTarget("_blank");

		final HorizontalLayout horizontalLinks = new HorizontalLayout();
		horizontalLinks.setPadding(true);
		horizontalLinks.add("Developed at the ");
		horizontalLinks.add(lablink);
		horizontalLinks.add(" at the ");
		horizontalLinks.add(scrippsLink);
		add(horizontalLinks);
		add(logo);
		// link to github
		final String url = "https://github.com/proteomicsyates/pctsea-parent";
		final Anchor link = new Anchor(url, url);
		link.setTarget("_blank");
		final HorizontalLayout horizontal = new HorizontalLayout();
		horizontal.setPadding(true);
		horizontal.add("Source code available at: ");
		horizontal.add(link);
		add(horizontal);

		try {

			final String pctseaResultsViewerURL = PCTSEALocalConfiguration.getPCTSEAResultsViewerURL();
			final String pctseaComparisonViewerURL = PCTSEALocalConfiguration.getPCTSEAResultsComparisonURL();
			final Details details = new Details();
			details.setSummaryText("Server status");
			final VerticalLayout verticalPanel = new VerticalLayout(
					new Label("URL for viewer of results: " + pctseaResultsViewerURL),
					new Label("URL for comparisons of results: " + pctseaComparisonViewerURL),
					new Label("Emails with the results will be sent by: " + PCTSEALocalConfiguration.getFromEmail()),
					new Label("Results are stored at: " + PCTSEALocalConfiguration.getPCTSEAResultsFolder()),
					new Label("Results comparisons are stored at: "
							+ PCTSEALocalConfiguration.getPCTSEAResultsComparisonFolder()));

			details.addContent(verticalPanel);
//			details.addThemeVariants(DetailsVariant.SMALL);
			add(details);
		} catch (final PCTSEAConfigurationException e) {
			add(new Label("There is some error in this server: " + e.getMessage()));
		}
		// runs
		final Details detailsRuns = new Details();
		detailsRuns.setSummaryText("Runs log");
		add(detailsRuns);
		final Details detailsRunsFinished = new Details();

		detailsRuns.addContent(detailsRunsFinished);

		final Details detailsRunsFailed = new Details();

		detailsRuns.addContent(detailsRunsFailed);

		final List<PctseaRunLog> runs = runRepo.findAll();
		Collections.reverse(runs);
		final Grid<PctseaRunLog> gridFinishedRuns = new Grid<>();

		final List<PctseaRunLog> finishedRuns = runs.stream().filter(run -> run.getFinished() != null)
				.collect(Collectors.toList());
		gridFinishedRuns.setItems(finishedRuns);
		detailsRunsFinished.setSummaryText("Finished runs: " + finishedRuns.size());
		gridFinishedRuns.addColumn(PctseaRunLog::getTimeStamp).setHeader("Time Stamp");
		gridFinishedRuns.addColumn(PctseaRunLog::getStarted).setHeader("Started");
		gridFinishedRuns.addColumn(PctseaRunLog::getFinished).setHeader("Finished");
		gridFinishedRuns.addColumn(new ValueProvider<PctseaRunLog, String>() {

			@Override
			public String apply(PctseaRunLog run) {
				if (run.getRunningTime() > 0l) {
					return DatesUtil.getDescriptiveTimeFromMillisecs(run.getRunningTime());
				}
				return "";
			}
		}).setHeader("Running time");
		gridFinishedRuns.addColumn(new ValueProvider<PctseaRunLog, String>() {

			@Override
			public String apply(PctseaRunLog run) {
				if (run.getInputParameters() != null && run.getInputParameters().getDatasets() != null) {
					final StringBuilder sb = new StringBuilder();
					for (final Dataset dataset : run.getInputParameters().getDatasets()) {
						if (!"".equals(sb.toString())) {
							sb.append(",");
						}
						sb.append(dataset.getTag());
					}
					return sb.toString();
				}
				return "";
			}
		}).setHeader("Datasets");
		gridFinishedRuns.addColumn(new ValueProvider<PctseaRunLog, String>() {

			@Override
			public String apply(PctseaRunLog run) {
				if (run.getInputParameters() != null && run.getInputParameters().getScoringSchemas() != null) {
					final StringBuilder sb = new StringBuilder();
					for (final ScoringSchema scoringSchema : run.getInputParameters().getScoringSchemas()) {
						if (!"".equals(sb.toString())) {
							sb.append(",");
						}
						sb.append(scoringSchema.getScoringMethod().getScoreName());
					}
					return sb.toString();
				}
				return "";
			}
		}).setHeader("Scoring");
		gridFinishedRuns.addColumn(new ValueProvider<PctseaRunLog, String>() {

			@Override
			public String apply(PctseaRunLog run) {
				if (run.getInputParameters() != null && run.getInputParameters().getEmail() != null) {
					return anonymizeEmail(run.getInputParameters().getEmail());
				}
				return "";
			}
		}).setHeader("Email");
		gridFinishedRuns.addColumn(new ValueProvider<PctseaRunLog, String>() {

			@Override
			public String apply(PctseaRunLog run) {
				if (run.getInputParameters() != null) {
					return String.valueOf(run.getInputParameters().getNumPermutations());
				}
				return "";
			}
		}).setHeader("# Permutations");
		gridFinishedRuns.addColumn(PctseaRunLog::getNumInputGenes).setHeader("# input genes");
		detailsRunsFinished.addContent(gridFinishedRuns);
		detailsRunsFinished.addThemeVariants(DetailsVariant.FILLED);

		final Grid<PctseaRunLog> gridFailingRuns = new Grid<>();
		final List<PctseaRunLog> failedRuns = runs.stream().filter(run -> run.getFinished() == null)
				.collect(Collectors.toList());
		detailsRunsFailed.setSummaryText("Failed runs: " + failedRuns.size());
		gridFailingRuns.setItems(failedRuns);
		gridFailingRuns.addColumn(PctseaRunLog::getTimeStamp).setHeader("Time Stamp");

		gridFailingRuns.addColumn(new ValueProvider<PctseaRunLog, String>() {

			@Override
			public String apply(PctseaRunLog run) {
				if (run.getInputParameters() != null && run.getInputParameters().getEmail() != null) {
					return anonymizeEmail(run.getInputParameters().getEmail());
				}
				return "";
			}
		}).setHeader("Email");
		detailsRunsFailed.addContent(gridFailingRuns);
		detailsRunsFailed.addThemeVariants(DetailsVariant.FILLED);

	}

	protected String anonymizeEmail(String email) {
		if (email.contains("@")) {
			final String[] split = email.split("@");
			final String userName = split[0];
			String domain = split[1];
			String extension = "";
			if (domain.contains(".")) {
				final String[] split2 = domain.split("\\.");
				extension = split2[1];
				domain = split2[0];
			}
			final StringBuilder sb = new StringBuilder();
			sb.append(getHalfRedacted(userName) + " @ " + getHalfRedacted(domain) + "." + extension);
			return sb.toString();
		} else {
			return getHalfRedacted(email);
		}
	}

	private String getHalfRedacted(String string) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			if (i <= string.length() / 2) {
				sb.append(string.charAt(i));
			} else {
				sb.append(".");
			}
		}
		return sb.toString();
	}
}
