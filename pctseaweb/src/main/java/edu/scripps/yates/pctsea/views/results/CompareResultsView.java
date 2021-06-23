package edu.scripps.yates.pctsea.views.results;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

import edu.scripps.yates.pctsea.comparator.PCTSEAMultipleResultsMerger;
import edu.scripps.yates.pctsea.util.PCTSEALocalConfiguration;
import edu.scripps.yates.pctsea.utils.CellTypesOutputTableColumns;
import edu.scripps.yates.pctsea.views.analyze.MyConfirmDialog;
import edu.scripps.yates.pctsea.views.main.MainView;

@PreserveOnRefresh
@Route(value = "compare", layout = MainView.class)
@PageTitle("PCTSEA web - Compare results")
@CssImport("./styles/views/results/results-view.css")
public class CompareResultsView extends VerticalLayout {

	/**
		 * 
		 */
	private static final long serialVersionUID = 4823065117108763681L;
	private Button compareButton;
	private TextArea textArea;
	private VerticalLayout resultsPanel;
	private ProgressBar progressBar;
	public static ExecutorService executor = Executors.newFixedThreadPool(40);

	public CompareResultsView() {
	}

	@PostConstruct
	public void init() {
		setId("results-view");

		final H3 title = new H3("Paste here the URL with the results that you want to compare:");
		add(title);
		textArea = new TextArea("URLs with results", "Paste here in multiple lines...");
		textArea.getStyle().set("minHeight", "350px");
		textArea.setWidth(100, Unit.PERCENTAGE);

		add(textArea);

		compareButton = new Button("Compare");
		compareButton.addClickListener(event -> {
			startComparison();
		});
		add(compareButton);
		final Button buttonTMP = new Button("Add test");
		buttonTMP.addClickListener(event -> {
			final String urls = "http://sealion.scripps.edu:3838/salvador/pctsea/?results=2021-06-18_15-05-37_Jolene_Brain_M-D-2-SPC5_correlation"
					+ "\n"
					+ "http://sealion.scripps.edu:3838/salvador/pctsea/?results=2021-06-18_15-55-22_Jolene_Brain_M-D-3_Pearson_correlation";
			textArea.setValue(urls);
		});
		add(buttonTMP);
		resultsPanel = new VerticalLayout();
		add(resultsPanel);
		final Div footer = new Div();
		add(footer);
	}

	private void startComparison() {
		try {

			progressBar = new ProgressBar(1, CellTypesOutputTableColumns.values().length, 1);

			final Div progressBarLabel = new Div();
			progressBarLabel.setText("Generating comparison...");
			resultsPanel.removeAll();
			resultsPanel.add(progressBarLabel, progressBar);

			compareButton.setEnabled(false);
			final List<String> resultNames = getResultNamesFromURLs(textArea.getValue());
			for (final String string : resultNames) {
				System.out.println(string);
			}
			final File pctseaResultsComparisonsFolder = PCTSEALocalConfiguration.getPCTSEAResultsComparisonFolder();
			if (pctseaResultsComparisonsFolder == null) {
				throw new IllegalArgumentException(
						"Something is not well configured in PCTSEA. There is not a configured results comparisons folder to look at.");
			}

			if (!pctseaResultsComparisonsFolder.exists()) {
				pctseaResultsComparisonsFolder.mkdirs();
			}
			final File pctseaResultsFolder = PCTSEALocalConfiguration.getPCTSEAResultsFolder();
			if (pctseaResultsFolder == null || !pctseaResultsFolder.exists()) {
				throw new IllegalArgumentException(
						"Something is not well configured in PCTSEA. There is not a configured results folder to look at.");
			}

			final List<File> zipFiles = getZipFilesFromResultsFolder(pctseaResultsFolder, resultNames);

			// generate unique code from the resultsNames
			final String comparisonFolderCode = getComparisonFolderCode(resultNames);
			final File resultsComparisonFolder = new File(pctseaResultsComparisonsFolder.getAbsolutePath()
					+ File.separator + "comparisons" + File.separator + comparisonFolderCode);
			// exists already? then redirect
			if (!resultsComparisonFolder.exists()) {
				final UI ui = UI.getCurrent();
				resultsComparisonFolder.mkdirs();
				final Double fdrThreshold = null;
				final PCTSEAMultipleResultsMerger resultsMerger = new PCTSEAMultipleResultsMerger(zipFiles,
						fdrThreshold, resultsComparisonFolder);
				final CellTypesOutputTableColumns[] values = CellTypesOutputTableColumns.values();

				final CellTypesOutputTableColumns[] cols = values;

				final Runnable job = new Runnable() {
					@Override
					public void run() {
						try {
							for (int i = 0; i < cols.length; i++) {
								final int value = i + 1;
								ui.access(() -> {
									progressBar.setValue(value);
								});
								final CellTypesOutputTableColumns col = cols[i];
								resultsMerger.run(col);
							}

							ui.access(() -> {
								// redirect to comparison shiny app
								final String pctseaComparisonViewerURL = PCTSEALocalConfiguration
										.getPCTSEAResultsComparisonURL();
								final String comparisonURL = pctseaComparisonViewerURL + "?f=" + comparisonFolderCode;
								resultsPanel.add(new Label("The comparison now is ready."));
								final Anchor link = new Anchor(comparisonURL, comparisonURL);
								link.setTarget("_blank");
								resultsPanel.add(new Label("Go to: "), link);

							});
						} catch (final Exception e) {
							e.printStackTrace();
						} finally {
							ui.access(() -> {
								progressBar.setValue(progressBar.getMax());
							});
						}
					}
				};
				executor.submit(job);
			} else {
				progressBar.setValue(progressBar.getMax());
				// redirect to comparison shiny app
				final String pctseaComparisonViewerURL = PCTSEALocalConfiguration.getPCTSEAResultsComparisonURL();
				final String comparisonURL = pctseaComparisonViewerURL + "?f=" + comparisonFolderCode;
				resultsPanel.add(new Label("The comparison now is ready."));
				final Anchor link = new Anchor(comparisonURL, comparisonURL);
				link.setTarget("_blank");
				resultsPanel.add(new Label("Go to: "), link);
			}

		} catch (final Exception e) {
			final MyConfirmDialog dialog = new MyConfirmDialog("Error occurred:", e.getMessage(), "OK");
			dialog.open();
		} finally {
			compareButton.setEnabled(true);
		}
	}

	private String getComparisonFolderCode(List<String> resultNames) {
		final StringBuilder sb = new StringBuilder();
		resultNames.stream().sorted().forEach(name -> sb.append(name));
		final String hash = Hashing.sha256().hashString(sb.toString(), Charsets.UTF_8).toString();
		return hash;
	}

	private List<File> getZipFilesFromResultsFolder(File pctseaResultsFolder, List<String> resultNames) {
		final List<File> ret = new ArrayList<File>();
		for (final String fileName : resultNames) {
			final File zipFile = new File(pctseaResultsFolder.getAbsolutePath() + File.separator + fileName + ".zip");
			if (!zipFile.exists()) {
				throw new IllegalArgumentException("File not found for result: '" + fileName + "'");
			}
			ret.add(zipFile);
		}
		return ret;
	}

	private List<String> getResultNamesFromURLs(String string) throws MalformedURLException, URISyntaxException {
		if (string.trim().equals("")) {
			throw new IllegalArgumentException("You must enter at least 2 URL with results to compare");
		}
		if (!string.contains("\n")) {
			throw new IllegalArgumentException("You must enter at least 2 URL with results to compare");
		} else {
			final String[] split = string.split("\n");
			final List<String> ret = new ArrayList<String>();
			for (final String urlString : split) {
				if ("".equals(urlString.trim())) {
					continue;
				}
				final String query = new URL(urlString).getQuery();
				final String[] params = query.split("&");

				boolean valid = false;
				for (final String param : params) {
					final String name = param.split("=")[0];
					final String value = param.split("=")[1];
					if ("results".equals(name)) {
						ret.add(value);
						valid = true;
					}
				}
				if (!valid) {
					throw new IllegalArgumentException("URL '" + urlString + "' doesn't contain 'results=XXXX'");
				}
			}
			return ret;
		}

	}
}
