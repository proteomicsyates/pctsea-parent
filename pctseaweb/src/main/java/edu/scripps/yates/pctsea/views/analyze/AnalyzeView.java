package edu.scripps.yates.pctsea.views.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.pctsea.util.PCTSEALocalConfiguration;
import edu.scripps.yates.pctsea.util.VaadinUtil;
import edu.scripps.yates.pctsea.views.main.MainView;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.swing.StatusListener;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.set.hash.THashSet;

@Route(value = "analyze", layout = MainView.class)
@PageTitle("pCtSEAweb - Analyze")
@CssImport("./styles/views/analyze/analyze-view.css")
public class AnalyzeView extends VerticalLayout {

	private final NumberField minCorrelation = new NumberField("Minimum Pearson's correlation");
	private final IntegerField minGenesCells = new IntegerField("Minimum number of proteins");
	private final ComboBox<Dataset> datasets = new ComboBox<Dataset>("Single cells dataset to compare against");
	private final TextField outputPrefix = new TextField("Prefix for all output files", "experiment1");
	private final EmailField email = new EmailField("Email", "your_email@domain.com");
	private final IntegerField numPermutations = new IntegerField("Number of permutations", "1000");
	MemoryBuffer buffer = new MemoryBuffer();
	private final MyUpload upload = new MyUpload(buffer);
	private final Div outputDiv = new Div();
	private final Button clearButton = new Button("Clear");
	private final Button cancelButton = new Button("Cancel");
	private final Button submitButton = new Button("Submit");
	private final TextArea statusArea = new TextArea("Progress status:");
	private Binder<InputParameters> binder;
	private File inputFile;
	private final int defaultMinGenes = 4;
	private final double defaultMinCorrelation = 0.1;
	private final int defaultPermutations = 1000;
	private Button showInputDataButton;
	private VerticalLayout inputFileDataTabContent;
	private Tab inputFileDataTab;
	private Map<Tab, Component> tabsToPages;
	private Tabs tabs;
	@Autowired
	private SingleCellMongoRepository scmr;
	@Autowired
	private ExpressionMongoRepository emr;
	@Autowired
	private PctseaRunLogRepository runLogsRepo;
	@Autowired
	private MongoBaseService mbs;
	@Autowired
	private DatasetMongoRepository dmr;

	private HorizontalLayout resultsPanel;
	private ExecutorService executor;

	class MyUpload extends Upload {
		private static final long serialVersionUID = 1L;

		public MyUpload(MemoryBuffer buffer) {
			super(buffer);
		}

		Registration addFileRemoveListener(ComponentEventListener<FileRemoveEvent> listener) {
			return super.addListener(FileRemoveEvent.class, listener);
		}
	}

	@DomEvent("file-remove")
	public static class FileRemoveEvent extends ComponentEvent<Upload> {
		public FileRemoveEvent(Upload source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public AnalyzeView() {
	}

	@PostConstruct
	public void init() {
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
		binder.forField(outputPrefix).asRequired("Required")
				.withValidator(prefix -> !"".equals(prefix), "Prefix is required").withValidator(prefix -> {
					final String tmp = FileUtils.checkInvalidCharacterNameForFileName(prefix);
					if (!tmp.equals(prefix)) {
						return false;
					}
					return true;
				}, "Prefix contains invalid characters")
				.bind(InputParameters::getOutputPrefix, InputParameters::setOutputPrefix);
		binder.forField(minGenesCells).asRequired("Required").withValidator(num -> num >= 1, "Minimum is 3")
				.bind(InputParameters::getMinGenesCells, InputParameters::setMinGenesCells);
		binder.forField(numPermutations).asRequired("Required")
				.withValidator(num -> num >= 10, "Minimum number of permutations: 10")
				.bind(InputParameters::getNumPermutations, InputParameters::setNumPermutations);
		binder.forField(minCorrelation).asRequired("Required")
				.withValidator(num -> num >= 0.0, "Minimum correlation is 0.0")
				.withValidator(num -> num <= 1.0, "Maximum correlation is 1.0")
				.bind(InputParameters::getMinCorrelation, InputParameters::setMinCorrelation);
		binder.forField(email).asRequired("Required")
				.withValidator(new EmailValidator("This doesn't look like a valid email address"))
				.bind(InputParameters::getEmail, InputParameters::setEmail);

		binder.forField(datasets).asRequired("Required").withValidator(dataset -> dataset != null,
				"A dataset must be selected");
		// load datasets
		loadDatasetsInComboList();

		// buttons
		clearButton.addClickListener(e -> clearForm());
		submitButton.addClickListener(e -> {
			try {
				submit();
			} catch (final Exception e2) {
				e2.printStackTrace();
				VaadinUtil.showErrorDialog("Error: " + e2.getMessage());
				return;
			}
		});
		cancelButton.addClickListener(e -> {
			final List<Runnable> jobsNotStarted = executor.shutdownNow();
			VaadinUtil.showInformationDialog("pCtSEA will stop soon...");
		});
		cancelButton.setEnabled(false);

		resultsPanel = new HorizontalLayout();
		resultsPanel.add(
				"Results will appear here as soon as the analysis is done. Also, an email will be sent to the provided email adress.");
		resultsPanel.setVisible(false);
		add(resultsPanel);
		initializeInputParamsToDefaults();
		statusArea.getStyle().set("minHeight", "150px");
		statusArea.setMaxHeight("300px");
		statusArea.setReadOnly(true);
		statusArea.setWidthFull();
		statusArea.setVisible(false);
		add(statusArea);
		final Div footer = new Div();
		add(footer);
	}

	private void loadDatasetsInComboList() {
		final List<Dataset> datasetsFromDB = dmr.findAll();

		this.datasets.setItems(datasetsFromDB);

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
		InputParameters inputParameters = new InputParameters();
		final boolean valid = binder.writeBeanIfValid(inputParameters);
		if (!valid) {
			inputParameters = null;
			VaadinUtil.showErrorDialog("Errors in input parameters!");
			return;
		}
		// TODO
		inputParameters.setCellTypesClassification("TYPE");
		inputParameters.setLoadRandom(false);
		inputParameters.setGenerateCharts(true);
		inputParameters.setPlotNegativeEnriched(false);
		inputParameters.setInputDataFile(inputFile.getAbsolutePath());
		inputParameters.setWriteCorrelationsFile(false);
		final Set<String> datasetTags = new THashSet<String>();
		final Dataset selectedDataset = this.datasets.getValue();
		if (selectedDataset == null) {
			inputParameters = null;
			VaadinUtil.showErrorDialog("Errors in input parameters: A dataset must be selected.");
			return;
		}
		datasetTags.add(selectedDataset.getTag());
		inputParameters.setDatasets(datasetTags);
		startPCTSEAAnalysis(inputParameters);
		Notification.show("Run starting...");

	}

	private void startPCTSEAAnalysis(InputParameters inputParameters) {
//		showSpinnerDialog();
		final UI ui = UI.getCurrent();
		final PCTSEA pctsea = new PCTSEA(inputParameters, emr, scmr, runLogsRepo, mbs);
		pctsea.setStatusListener(new StatusListener() {

			@Override
			public void onStatusUpdate(String statusMessage) {
				ui.access(() -> {
					showMessage(statusMessage);
				});
			}
		});
		try {
			pctsea.setResultsViewerURL(PCTSEALocalConfiguration.getPCTSEAResultsViewerURL());
			final String fromEmail = PCTSEALocalConfiguration.getFromEmail();
			pctsea.setFromEmail(fromEmail);
		} catch (MalformedURLException | URISyntaxException e1) {
			// this shoudn't happen because it is already checked before, but just in case
			// we throw a runtimeException
			throw new RuntimeException(e1);
		}
		setEnabledStatusAsRunning();
		statusArea.setValue("Starting run...");

		// launch this in background
		executor = Executors.newSingleThreadExecutor();

		final Runnable currentPSEAJob = new Runnable() {

			@Override
			public void run() {

				try {
					final PCTSEAResult results = pctsea.run();
					ui.access(() -> {
						showLinkToResults(results.getUrlToViewer());
					});

				} catch (final RuntimeException e) {
					ui.access(() -> {
						VaadinUtil.showErrorDialog("pCtSEA has stopped.");
						showMessage("pCtSEA has stopped.");
					});
				} finally {
					ui.access(() -> {
						setEnabledStatusAsReady();
					});
				}

			}

		};
		executor.submit(currentPSEAJob);

	}

	private void setEnabledStatusAsRunning() {
		submitButton.setEnabled(false);
		cancelButton.setEnabled(true);
		clearButton.setEnabled(false);
		this.minCorrelation.setEnabled(false);
		this.minGenesCells.setEnabled(false);
		this.numPermutations.setEnabled(false);
		this.outputPrefix.setEnabled(false);
		this.statusArea.setVisible(true);
		this.resultsPanel.setVisible(true);
	}

	private void setEnabledStatusAsReady() {
		submitButton.setEnabled(true);
		cancelButton.setEnabled(false);
		clearButton.setEnabled(true);
		this.minCorrelation.setEnabled(true);
		this.minGenesCells.setEnabled(true);
		this.numPermutations.setEnabled(true);
		this.outputPrefix.setEnabled(true);
	}

	protected void showLinkToResults(URL url) {
		if (url != null) {
			final Anchor link = new Anchor(url.toString(), url.toString());
			resultsPanel.removeAll();
			resultsPanel.add("Access your results at: ");
			resultsPanel.add(link);
		} else {
			resultsPanel.removeAll();
			resultsPanel.add("Your results cannot be accessed by our web. Hopefully you got them by email.");
		}
	}

	private static final SimpleDateFormat timeformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	protected void showMessage(String statusMessage) {
		statusArea.setValue(statusArea.getValue() + "\n" + timeformatter.format(new Date()) + ": " + statusMessage);
	}

	private void initializeInputParamsToDefaults() {
		//
		outputPrefix.setHelperText("All output files will be named with that prefix on them");
		//
		minCorrelation.setHelperText("Values from 0.0 to 1.0");
		minCorrelation.setValue(defaultMinCorrelation);
		minCorrelation.setMax(1.0);
		minCorrelation.setMin(0.0);
		//
		minGenesCells.setValue(defaultMinGenes);
		minGenesCells
				.setHelperText("Minimum number of proteins that should have a non-zero expression value in a cell. "
						+ "Minimum value: 2");
		minGenesCells.setMin(2);
		//
		numPermutations.setValue(defaultPermutations);
		numPermutations.setMin(10);
		numPermutations.setHelperText(
				"Number of permutations for calculating significance of the enrichment scores, being a value of 1000 reasonable. Minimum value: 10");
		//
		this.datasets.setAutoOpen(true);
		this.datasets.setHelperText(
				"Datasets stored in the database that can be used to compare your data against. Select the one that is more appropiate to your input.");
		this.datasets.setClearButtonVisible(true);
		this.datasets.setItemLabelGenerator(new ItemLabelGenerator<Dataset>() {

			@Override
			public String apply(Dataset item) {
				return item.getTag() + ": " + item.getName();
			}
		});
		this.datasets.setPlaceholder("Select dataset");
		this.datasets.addValueChangeListener(event -> {
			final Dataset value = event.getValue();
			if (value != null) {
				// TODO
				// show information about it on other components
			}

		});
		//
		upload.setMaxFiles(1);
		final int maxFileSizeInBytes = 100 * 1024 * 1024; // 100Mb
		upload.setMaxFileSize(maxFileSizeInBytes);
		upload.setMinWidth("25em");
		upload.setDropAllowed(true);

		upload.addFileRejectedListener(event -> {
			VaadinUtil.showErrorDialog(event.getErrorMessage());
		});

		upload.addFinishedListener(event -> {

			try {
				// save input File to the results folder using PCTSEALocalConfiguration:
				final String fileName = event.getFileName();
				final File pctseaResultsFolder = PCTSEALocalConfiguration.getPCTSEAResultsFolder();
				if (pctseaResultsFolder != null) {
					inputFile = new File(pctseaResultsFolder.getAbsolutePath() + File.separator + fileName);
				} else {
					inputFile = Files.createTempFile("pctsea", "upload.txt").toFile();
				}

				//////////////////////////////////////
				// make sure the input file is unique:
				File tmpFile = inputFile;
				int version = 0;
				while (tmpFile.exists()) {
					version++;
					tmpFile = new File(inputFile.getParent() + File.separator
							+ FilenameUtils.getBaseName(inputFile.getAbsolutePath()) + "_" + version + "."
							+ FilenameUtils.getExtension(inputFile.getAbsolutePath()));
				}
				inputFile = tmpFile;
				//////////////////////////////////////
				final FileOutputStream outputStream = new FileOutputStream(inputFile);
				IOUtils.copy(buffer.getInputStream(), outputStream);
				outputStream.close();
				final List<Pair> genes = validateInputFile(inputFile);
				enableInputFileDataTab(genes);
				outputDiv.removeAll();
				outputDiv.add(genes.size()
						+ " genes/proteins read successfully from input file. You can review the input data in the 'Input data' tab below:");
				showInputDataButton.setEnabled(true);

			} catch (final Exception e) {
				VaadinUtil.showErrorDialog("Error validating input file: " + e.getMessage());
				inputFile = null;
			}
		});
		upload.addFileRemoveListener(event -> {
			outputDiv.removeAll();
			inputFileDataTabContent.removeAll();
			if (inputFile != null && inputFile.exists()) {
				inputFile.delete();
			}
			inputFile = null;
			showInputDataButton.setEnabled(false);
		});
		//
		email.setHelperText(
				"Include your email in case the analysis take more than a few minutes. You will get notified with the results by email.");
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
		bean.setEmail(null);
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
		formLayout.add(datasets);
		formLayout.add(minCorrelation);
		formLayout.add(minGenesCells);
		formLayout.add(outputPrefix);
		formLayout.add(numPermutations);
		formLayout.add(email);
		return formLayout;
	}

	private Component createButtonLayout() {
		final HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.addClassName("button-layout");
		submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		buttonLayout.add(submitButton);
		buttonLayout.add(clearButton);
		buttonLayout.add(cancelButton);
		cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		return buttonLayout;
	}

	private Component createUploadLayout() {
		final VerticalLayout uploadLayout = new VerticalLayout();
//		uploadLayout.addClassName("button-layout");

		final Label label = new Label(
				"Tab-separated text file with two columns, one the protein accessions (UniprotKB) or gene symbols "
						+ "and the second, the quantitative values. Any other column will be ignored.");
		uploadLayout.add(label);

		final HorizontalLayout horizontal = new HorizontalLayout();
		final VerticalLayout verticalLayout2 = new VerticalLayout();
		verticalLayout2.add(upload, outputDiv);
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
