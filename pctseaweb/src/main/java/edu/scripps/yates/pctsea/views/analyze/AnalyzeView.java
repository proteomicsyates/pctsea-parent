package edu.scripps.yates.pctsea.views.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.util.VaadinUtil;
import edu.scripps.yates.pctsea.utils.StatusListener;
import edu.scripps.yates.pctsea.views.main.MainView;
import edu.scripps.yates.utilities.util.Pair;

@Route(value = "analyze", layout = MainView.class)
@PageTitle("Analyze")
@CssImport("./styles/views/analyze/analyze-view.css")
public class AnalyzeView extends VerticalLayout {

	private final NumberField minCorrelation = new NumberField("Minimum Pearson's correlation");
	private final IntegerField minGenesCells = new IntegerField("Minimum number of proteins");
	private final TextField outputPrefix = new TextField("Prefix for all output files", "experiment1");
	private final IntegerField numPermutations = new IntegerField("Number of permutations", "1000");
	MemoryBuffer buffer = new MemoryBuffer();
	private final Upload upload = new Upload(buffer);
	private final Div output = new Div();
	private final Button cancelButton = new Button("Clear");
	private final Button submitButton = new Button("Submit");
	private final TextArea statusArea = new TextArea("Progress status:");
	private final Binder<InputParameters> binder;
	private File inputFile;
	private final int defaultMinGenes = 4;
	private final double defaultMinCorrelation = 0.1;
	private final int defaultPermutations = 1000;
	private Button showInputDataButton;
	private final VerticalLayout inputFileDataTabContent;
	private final Tab inputFileDataTab;
	private final Map<Tab, Component> tabsToPages;
	private final Tabs tabs;
	@Autowired
	private SingleCellMongoRepository scmr;
	@Autowired
	private ExpressionMongoRepository emr;
	private Runnable backgroundProcess;

	public AnalyzeView() {
		setId("analyze-view");

		add(createTitle());
		add(new H4("Input file"));
		add(createUploadLayout());

		// tabs

		tabsToPages = new HashMap<Tab, Component>();
		final Tab inputParametersTab = new Tab("Input parameters");
		inputParametersTab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
		final VerticalLayout inputParametersTabContent = new VerticalLayout(createFormLayout(), createButtonLayout());
		tabsToPages.put(inputParametersTab, inputParametersTabContent);
		inputFileDataTab = new Tab("Input data");
		inputFileDataTab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
		inputFileDataTab.setEnabled(false);
		inputFileDataTabContent = new VerticalLayout();
		inputFileDataTabContent.setVisible(false);
		tabsToPages.put(inputFileDataTab, inputFileDataTabContent);

		tabs = new Tabs(inputParametersTab, inputFileDataTab);
		tabs.getStyle().set("max-width", "100%");
		final Div pages = new Div(inputParametersTabContent, inputFileDataTabContent);
		add(tabs, pages);
		tabs.addSelectedChangeListener(event -> {
			showTabContent(tabs.getSelectedTab());

		});

		binder = new Binder<>(InputParameters.class);
		final InputParameters inputParameters = new InputParameters();
		binder.setBean(inputParameters);
		binder.forField(this.outputPrefix).asRequired("Required")
				.withValidator(prefix -> !"".equals(prefix), "Prefix is required")
				.bind(InputParameters::getOutputPrefix, InputParameters::setOutputPrefix);
		binder.forField(this.minGenesCells).asRequired("Required").withValidator(num -> num >= 1, "Minimum is 1")
				.bind(InputParameters::getMinGenesCells, InputParameters::setMinCellsPerCellType);
		binder.forField(this.numPermutations).asRequired("Required")
				.withValidator(num -> num >= 10, "Minimum number of permutations: 10")
				.bind(InputParameters::getNumPermutations, InputParameters::setNumPermutations);
		binder.forField(this.minCorrelation).asRequired("Required")
				.withValidator(num -> num >= 0.0, "Minimum correlation is 0.0")
				.withValidator(num -> num <= 1.0, "Maximum correlation is 1.0")
				.bind(InputParameters::getMinCorrelation, InputParameters::setMinCorrelation);

		cancelButton.addClickListener(e -> clearForm());
		submitButton.addClickListener(e -> {
			submit();
		});

		initializeInputParamsToDefaults();
		statusArea.getStyle().set("minHeight", "150px");
		statusArea.setWidthFull();
		add(statusArea);
	}

	private void showTabContent(Tab selectedTab) {
		tabsToPages.values().forEach(content -> content.setVisible(false));
		final Component selectedContent = tabsToPages.get(selectedTab);
		selectedContent.setVisible(true);

	}

	private void submit() {
		if (inputFile == null || !inputFile.exists()) {
			VaadinUtil.showErrorDialog("Input file is missing!");
			return;
		}
		final InputParameters inputParameters = binder.getBean();
		inputParameters.setCellTypesClassification("TYPE");
		inputParameters.setLoadRandom(false);
		inputParameters.setGenerateCharts(true);
		inputParameters.setPlotNegativeEnriched(false);
		inputParameters.setInputDataFile(inputFile);
		startPCTSEAAnalysis(inputParameters);
		Notification.show("Run starting...");

	}

	private void startPCTSEAAnalysis(InputParameters inputParameters) {
//		showSpinnerDialog();
		final PCTSEA pTCPctsea = new PCTSEA(inputParameters, emr, scmr);
		pTCPctsea.setStatusListener(new StatusListener() {

			@Override
			public void onStatusUpdate(String statusMessage) {
				getUI().get().access(() -> showMessage(statusMessage));

			}
		});

		// launch this in background
		backgroundProcess = new Runnable() {

			@Override
			public void run() {

				getUI().get().access(() -> AnalyzeView.this.submitButton.setEnabled(false));
				final File resultsFile = pTCPctsea.run();
				getUI().get().access(() -> AnalyzeView.this.submitButton.setEnabled(true));
				copyResultToShinyFolder(resultsFile);
				showLinkToResults(resultsFile);
			}
		};
		backgroundProcess.run();

	}

	protected void showLinkToResults(File resultsFile) {
		// TODO Auto-generated method stub

	}

	protected void copyResultToShinyFolder(File resultsFile) {
		final File outputFile = new File("C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR");
		try {
			IOUtils.copy(new FileInputStream(resultsFile), new FileOutputStream(outputFile));
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	protected void showMessage(String statusMessage) {
		this.statusArea.setValue(this.statusArea.getValue() + "\n" + statusMessage);
	}

	private void initializeInputParamsToDefaults() {
		//
		outputPrefix.setHelperText("All output files will be named with that prefix on them");
		//
		this.minCorrelation.setHelperText("Values from 0.0 to 1.0");
		this.minCorrelation.setValue(defaultMinCorrelation);
		this.minCorrelation.setMax(1.0);
		this.minCorrelation.setMin(0.0);
		//
		this.minGenesCells.setValue(defaultMinGenes);
		this.minGenesCells.setHelperText(
				"Minimum number of proteins that should have a non-zero expression value in a cell. Minimum value: 1");
		this.minGenesCells.setMin(1);
		//
		this.numPermutations.setValue(defaultPermutations);
		this.numPermutations.setMin(10);
		this.numPermutations.setHelperText(
				"Number of permutations for calculating significance of the enrichment scores, being a value of 1000 reasonable. Minimum value: 10");
		//
		this.upload.setMaxFiles(1);
		final int maxFileSizeInBytes = 100 * 1024 * 1024; // 100Mb
		this.upload.setMaxFileSize(maxFileSizeInBytes);
		this.upload.setMinWidth("25em");
		this.upload.setDropAllowed(true);

		upload.addFileRejectedListener(event -> {
			VaadinUtil.showErrorDialog(event.getErrorMessage());
		});
		upload.addFinishedListener(event -> {

			try {
				inputFile = Files.createTempFile("pctsea", "upload.txt").toFile();

				final FileOutputStream outputStream = new FileOutputStream(inputFile);
				IOUtils.copy(buffer.getInputStream(), outputStream);
				outputStream.close();
				final List<Pair> genes = validateInputFile(inputFile);
				enableInputFileDataTab(genes);
				output.add(genes.size()
						+ " genes/proteins read successfully from input file. You can review the input data in the 'Input data' tab below:");
				showInputDataButton.setEnabled(true);

			} catch (final Exception e) {
				VaadinUtil.showErrorDialog("Error validating input file: " + e.getMessage());
				this.inputFile = null;
			}
		});
//		upload.addSucceededListener(event -> {
//			final Component component = VaadinUtil.createComponent(event.getMIMEType(), event.getFileName(),
//					buffer.getInputStream());
//			showOutput(event.getFileName(), component, output);
//		});
	}

	private void enableInputFileDataTab(List<Pair> genes) {
		if (genes == null) {
			inputFileDataTab.setEnabled(false);
		} else {
			inputFileDataTab.setEnabled(true);
			final Grid<Pair> grid = new Grid<Pair>();
			grid.addColumn(pair -> pair.getFirstelement()).setHeader("Protein/Gene").setSortable(true);
			grid.addColumn(pair -> pair.getSecondElement()).setHeader("Expression value").setSortable(true);
			grid.setItems(genes);
			final Text text = new Text("Data captured from uploaded input file (" + genes.size() + " proteins/genes)");
			inputFileDataTabContent.removeAll();
			inputFileDataTabContent.add(text, grid);
		}
	}

	private List<Pair> validateInputFile(File inputFile)

			throws Exception {
		final List<Pair> genes = new ArrayList<Pair>();

		BufferedReader reader = null;
		int numLine = 1;

		try {
			reader = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				try {
					if ("".equals(line.trim())) {
						continue;
					}
					if (line.contains("\t")) {
						final String[] split = line.split("\t");

						if (numLine == 1) {
							continue;
						}
						Pair<String, Double> pair = null;
						final String geneName = split[0];

						if (split.length < 2) {
							throw new Exception("Second column in missing in input file (line " + numLine + ")");
						}
						try {
							final Double expressionValue = Double.valueOf(split[1]);
							if (!"".equals(geneName)) {
								pair = new Pair<String, Double>(geneName, expressionValue);
								genes.add(pair);
							}
						} catch (final NumberFormatException e) {
							throw new Exception("Cannot process " + split[1] + " as a number (line " + numLine + ")");
						}
					}
				} finally {
					numLine++;
				}
			}
		} finally {
			reader.close();
		}
		if (genes.isEmpty() && numLine > 0) {
			throw new Exception("Looks like input file is not TAB separated?");
		}
		return genes;
	}

	private void showOutput(String text, Component content, HasComponents outputContainer) {
		final HtmlComponent p = new HtmlComponent(Tag.P);
		p.getElement().setText(text);
		outputContainer.add(p);
		outputContainer.add(content);
	}

	private void clearForm() {
		final InputParameters bean = new InputParameters();
		bean.setMinGenesCells(defaultMinGenes);
		bean.setMinCorrelation(defaultMinCorrelation);
		bean.setNumPermutations(defaultPermutations);
		binder.setBean(bean);
		inputFileDataTab.setEnabled(false);
		inputFileDataTabContent.removeAll();
	}

	private Component createTitle() {
		return new H3("pCtSEA input parameters");
	}

	private Component createFormLayout() {
		final FormLayout formLayout = new FormLayout();
//		email.setErrorMessage("Please enter a valid email address");

		formLayout.add(minCorrelation);
		formLayout.add(this.minGenesCells);
		formLayout.add(this.outputPrefix);
		formLayout.add(this.numPermutations);
		return formLayout;
	}

	private Component createButtonLayout() {
		final HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.addClassName("button-layout");
		submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		buttonLayout.add(submitButton);
		buttonLayout.add(cancelButton);
		return buttonLayout;
	}

	private Component createUploadLayout() {
		final VerticalLayout uploadLayout = new VerticalLayout();
//		uploadLayout.addClassName("button-layout");

		final Label label = new Label(
				"Tab-separated text file with two columns, one the protein or gene identified and the second, the quantitative information (spectral counts):");
		uploadLayout.add(label);

		final HorizontalLayout horizontal = new HorizontalLayout();
		final VerticalLayout verticalLayout2 = new VerticalLayout();
		verticalLayout2.add(upload, output);
		horizontal.add(verticalLayout2);

		showInputDataButton = new Button("Show input data");
		showInputDataButton.setWidthFull();
		showInputDataButton.setEnabled(false);
		showInputDataButton.addClickListener(event -> {
			tabs.setSelectedTab(inputFileDataTab);
		});
		horizontal.add(showInputDataButton);
		horizontal.setAlignSelf(Alignment.CENTER, showInputDataButton);
		uploadLayout.add(horizontal);
		return uploadLayout;
	}

}
