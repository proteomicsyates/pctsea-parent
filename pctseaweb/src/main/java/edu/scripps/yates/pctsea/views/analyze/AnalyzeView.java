package edu.scripps.yates.pctsea.views.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.component.upload.Receiver;
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
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.pctsea.model.ScoringMethod;
import edu.scripps.yates.pctsea.util.PCTSEALocalConfiguration;
import edu.scripps.yates.pctsea.util.VaadinUtil;
import edu.scripps.yates.pctsea.views.main.MainView;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.swing.StatusListener;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

@Route(value = "analyze", layout = MainView.class)
@PageTitle("PCTSEA web - Analyze")
@CssImport("./styles/views/analyze/analyze-view.css")
public class AnalyzeView extends VerticalLayout {

	private final NumberField minCorrelationField = new NumberField("Minimum Pearson's correlation");
	private final IntegerField minGenesCellsField = new IntegerField("Minimum number of proteins");
	private final ComboBox<Dataset> datasetsCombo = new ComboBox<Dataset>("Single cells dataset to compare against");
	private final ComboBox<CellTypeBranch> cellTypeBranchCombo = new ComboBox<CellTypeBranch>(
			"Level of cell type classification");
	private final ComboBox<ScoringMethod> scoringMethodCombo = new ComboBox<ScoringMethod>("Scoring method");
	private final TextField outputPrefixField = new TextField("Prefix for all output files", "experiment1");
	private final EmailField emailField = new EmailField("Email", "your_email@domain.com");
	private final IntegerField numPermutationsField = new IntegerField("Number of permutations", "1000");
//	private final Checkbox generatePDFCheckbox = new Checkbox("Generate PDF files with charts", false);
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
	private List<Dataset> datasetsFromDB;
	private VerticalLayout inputParametersTabContent;
	private boolean wasInNewLine;

	class MyUpload extends Upload {
		private static final long serialVersionUID = 1L;

		public MyUpload(Receiver buffer) {
			super(buffer);
			setMaxFileSize(1.0 * 100 * 1024 * 1024); // 100 Mb

			final Div output = new Div();
			addFileRejectedListener(event -> {
				final Paragraph component = new Paragraph();
				showOutput(event.getErrorMessage(), component, output);
			});
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
		inputParametersTabContent = new VerticalLayout(createFormLayout(), createButtonLayout());
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

		// load cellTypeBranches
		cellTypeBranchCombo.setItems(CellTypeBranch.values());
		// load scoring methods
		scoringMethodCombo.setItems(ScoringMethod.values());

		binder = new Binder<>(InputParameters.class);
		final InputParameters inputParameters = new InputParameters();
		binder.setBean(inputParameters);
		binder.forField(outputPrefixField).asRequired("Required")
				.withValidator(prefix -> !"".equals(prefix), "Prefix is required").withValidator(prefix -> {
					final String tmp = FileUtils.checkInvalidCharacterNameForFileName(prefix);
					if (!tmp.equals(prefix)) {
						return false;
					}
					return true;
				}, "Prefix contains invalid characters")
				.bind(InputParameters::getOutputPrefix, InputParameters::setOutputPrefix);
		binder.forField(minGenesCellsField).asRequired("Required").withValidator(num -> num >= 1, "Minimum is 3")
				.bind(InputParameters::getMinGenesCells, InputParameters::setMinGenesCells);
		binder.forField(numPermutationsField).asRequired("Required")
				.withValidator(num -> num >= 10, "Minimum number of permutations: 10")
				.bind(InputParameters::getNumPermutations, InputParameters::setNumPermutations);
		binder.forField(minCorrelationField).asRequired("Required")
				.withValidator(num -> num >= 0.0, "Minimum correlation is 0.0")
				.withValidator(num -> num <= 1.0, "Maximum correlation is 1.0")
				.bind(InputParameters::getMinCorrelation, InputParameters::setMinCorrelation);
		binder.forField(emailField).asRequired("Required")
				.withValidator(new EmailValidator("This doesn't look like a valid email address"))
				.bind(InputParameters::getEmail, InputParameters::setEmail);

		binder.forField(cellTypeBranchCombo).asRequired("Required")
				.withValidator(cellTypeBranch -> cellTypeBranch != null,
						"A cell type classification level must be selected.")
				.bind(InputParameters::getCellTypeBranch, InputParameters::setCellTypeBranch);

		binder.forField(scoringMethodCombo).asRequired("Required")
				.withValidator(scoringMethod -> scoringMethod != null, "A scoring method must be selected.")
				.bind(InputParameters::getScoringMethod, InputParameters::setScoringMethod);

		binder.forField(datasetsCombo).asRequired("Required")
				.withValidator(dataset -> dataset != null, "A dataset must be selected")
				.bind(InputParameters::getDataset, InputParameters::setDataset);

//		binder.forField(generatePDFCheckbox).bind(InputParameters::isGeneratePDFCharts,
//				InputParameters::setGeneratePDFCharts);

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
				"Results will appear here as soon as the analysis is done. Also, an email will be sent to the provided email address.");
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
		datasetsFromDB = dmr.findAll();

		datasetsCombo.setItems(datasetsFromDB);

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
		inputParameters.setLoadRandom(false);
		inputParameters.setPlotNegativeEnriched(false);
		inputParameters.setInputDataFile(inputFile.getAbsolutePath());
		inputParameters.setWriteCorrelationsFile(false);

		startPCTSEAAnalysis(inputParameters);
		Notification.show("Run starting. See below its progress..");

	}

	private void startPCTSEAAnalysis(InputParameters inputParameters) {
//		showSpinnerDialog();
		final UI ui = UI.getCurrent();
		final PCTSEA pctsea = new PCTSEA(inputParameters, emr, scmr, runLogsRepo, dmr, mbs);
		pctsea.setStatusListener(new StatusListener<Boolean>() {

			@Override
			public void onStatusUpdate(String statusMessage, Boolean inNewLine) {
				ui.access(() -> {
					showMessage(statusMessage, inNewLine);
				});
			}

			@Override
			public void onStatusUpdate(String statusMessage) {
				ui.access(() -> {
					showMessage(statusMessage, true);
				});
			}
		});

		pctsea.setResultsViewerURL(PCTSEALocalConfiguration.getPCTSEAResultsViewerURL());
		final String fromEmail = PCTSEALocalConfiguration.getFromEmail();
		pctsea.setFromEmail(fromEmail);

		setEnabledStatusAsRunning();
		// hide input parameters
		inputParametersTabContent.setVisible(false);
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

				} catch (final Exception e) {
					e.printStackTrace();
					ui.access(() -> {
						VaadinUtil.showErrorDialog("PCTSEA has stopped because: " + e.getMessage());
						showMessage("PCTSEA has stopped.", true);
					});
				} finally {
					ui.access(() -> {
						setEnabledStatusAsReady();
						// show again input parameters
						inputParametersTabContent.setVisible(true);
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
		minCorrelationField.setEnabled(false);
		minGenesCellsField.setEnabled(false);
		numPermutationsField.setEnabled(false);
		outputPrefixField.setEnabled(false);
		statusArea.setVisible(true);
		resultsPanel.setVisible(true);
		emailField.setEnabled(false);
		datasetsCombo.setEnabled(false);
		scoringMethodCombo.setEnabled(false);
		cellTypeBranchCombo.setEnabled(false);
//		generatePDFCheckbox.setEnabled(false);
	}

	private void setEnabledStatusAsReady() {
		submitButton.setEnabled(true);
		cancelButton.setEnabled(false);
		clearButton.setEnabled(true);
		minCorrelationField.setEnabled(true);
		minGenesCellsField.setEnabled(true);
		numPermutationsField.setEnabled(true);
		outputPrefixField.setEnabled(true);
		emailField.setEnabled(true);
		datasetsCombo.setEnabled(true);
		scoringMethodCombo.setEnabled(true);
		cellTypeBranchCombo.setEnabled(true);
//		generatePDFCheckbox.setEnabled(true);
	}

	protected void showLinkToResults(URL url) {
		if (url != null) {
			final Anchor link = new Anchor(url.toString(), url.toString());
			link.setTarget("_blank");
			resultsPanel.removeAll();
			resultsPanel.add("Access your results at: ");
			resultsPanel.add(link);
		} else {
			resultsPanel.removeAll();
			resultsPanel.add("Your results cannot be accessed by our web. Hopefully you got them by email.");
		}
	}

	private static final SimpleDateFormat timeformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	private static final String NOT_MAPPED = "not mapped!";

	protected void showMessage(String statusMessage, boolean inNewLine) {
		if (inNewLine || wasInNewLine) {
			statusArea.setValue(statusArea.getValue() + "\n" + timeformatter.format(new Date()) + ": " + statusMessage);
		} else {
			final String[] split = statusArea.getValue().split("\n");

			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < split.length - 1; i++) {
				sb.append(split[i] + "\n");
			}
			sb.append(timeformatter.format(new Date()) + ": " + statusMessage);
			statusArea.setValue(sb.toString());
		}
		wasInNewLine = inNewLine;
	}

	private void initializeInputParamsToDefaults() {
		//
		outputPrefixField.setHelperText("All output files will be named with that prefix on them");
		//
		minCorrelationField.setHelperText("Values from 0.0 to 1.0");
		minCorrelationField.setValue(defaultMinCorrelation);
		minCorrelationField.setMax(1.0);
		minCorrelationField.setMin(0.0);
		//
		minGenesCellsField.setValue(defaultMinGenes);
		minGenesCellsField
				.setHelperText("Minimum number of proteins that should have a non-zero expression value in a cell. "
						+ "Minimum value: 2");
		minGenesCellsField.setMin(2);
		//
		numPermutationsField.setValue(defaultPermutations);
		numPermutationsField.setMin(10);
		numPermutationsField.setHelperText(
				"Number of permutations for calculating significance of the enrichment scores, being a value of 1000 reasonable. Minimum value: 10");
		//
		datasetsCombo.setAutoOpen(true);
		datasetsCombo.setHelperText(
				"Datasets stored in the database that can be used to compare your data against. Select the one that is more appropiate to your input.");
		datasetsCombo.setClearButtonVisible(true);
		datasetsCombo.setItemLabelGenerator(new ItemLabelGenerator<Dataset>() {

			@Override
			public String apply(Dataset item) {
				return item.getTag() + ": " + item.getName();
			}
		});
		datasetsCombo.setPlaceholder("Select dataset");
		datasetsCombo.addValueChangeListener(event -> {
			final Dataset value = event.getValue();
			if (value != null) {
				// TODO
				// show information about it on other components
			}

		});
		// if there is onlly one item, select it
		if (datasetsFromDB != null && datasetsFromDB.size() == 1) {
			datasetsCombo.setValue(datasetsFromDB.get(0));
		}

		//
		cellTypeBranchCombo.setAutoOpen(true);
		cellTypeBranchCombo.setHelperText(
				"Level of cell type classification according to the hierarchical structure of its classification. Consult the administrator to know more about it.");
		cellTypeBranchCombo.setClearButtonVisible(true);
		cellTypeBranchCombo.setItemLabelGenerator(new ItemLabelGenerator<CellTypeBranch>() {

			@Override
			public String apply(CellTypeBranch item) {
				return item.name();
			}
		});
		cellTypeBranchCombo.setPlaceholder("Select level");
		cellTypeBranchCombo.addValueChangeListener(event -> {
			final CellTypeBranch value = event.getValue();
			if (value != null) {
				// TODO
				// show information about it on other components
			}

		});
		cellTypeBranchCombo.setValue(CellTypeBranch.TYPE);
		//
		//
		scoringMethodCombo.setAutoOpen(true);
		scoringMethodCombo.setHelperText(
				"Scoring method used to measure similarities between the input quantitative values and the single cell expressions.");
		scoringMethodCombo.setClearButtonVisible(true);
		scoringMethodCombo.setItemLabelGenerator(new ItemLabelGenerator<ScoringMethod>() {

			@Override
			public String apply(ScoringMethod item) {
				return item.name();
			}
		});
		scoringMethodCombo.setPlaceholder("Select level");
		scoringMethodCombo.addValueChangeListener(event -> {
			final ScoringMethod value = event.getValue();
			if (value != null) {
				if (value == ScoringMethod.MORPHEUS) {
					final ConfirmDialog dialog = new ConfirmDialog("Experimental scoring method",
							"The selected scoring method is still experimental", "OK", null);
					dialog.open();
				} else if (value != ScoringMethod.PEARSONS_CORRELATION) {
					final ConfirmDialog dialog = new ConfirmDialog("Scoring method not supported yet",
							"The selected scoring method is not supported yet", "OK", null);
					dialog.open();
					scoringMethodCombo.setValue(ScoringMethod.PEARSONS_CORRELATION);
				}
			}

		});
		scoringMethodCombo.setValue(ScoringMethod.PEARSONS_CORRELATION);
		//
		//
		upload.setMaxFiles(1);
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
					if (!pctseaResultsFolder.exists()) {
						pctseaResultsFolder.mkdirs();
					}
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
				final List<MappingRow> rows = validateInputFile(inputFile);
				enableInputFileDataTab(rows);
				outputDiv.removeAll();
				int numNotMapped = 0;
				for (final Dataset dataset : datasetsFromDB) {
					numNotMapped += getNumNotMapped(rows, dataset.getTag());

				}
				if (numNotMapped == 0) {
					outputDiv.add(rows.size()
							+ " genes/proteins read successfully from input file. You can review the input data in the 'Input data' tab below:");
				} else {
					outputDiv.add(rows.size()
							+ " genes/proteins read from input file. However, some of them were not found in some of the datasets. You can review the mapping in the 'Input data' tab below:");
				}
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
		emailField.setHelperText(
				"Include your email in case the analysis take more than a few minutes. You will get notified with the results by email.");
//		upload.addSucceededListener(event -> {
//			final Component component = VaadinUtil.createComponent(event.getMIMEType(), event.getFileName(),
//					buffer.getInputStream());
//			showOutput(event.getFileName(), component, output);
//		});
	}

	private void enableInputFileDataTab(List<MappingRow> rows) {
		if (rows == null || rows.isEmpty()) {
			inputFileDataTab.setEnabled(false);
		} else {
			inputFileDataTab.setEnabled(true);
			final Grid<MappingRow> grid = new Grid<MappingRow>();
			grid.addColumn(row -> row.getInputProteinGene()).setHeader("Protein/Gene").setSortable(true);
			grid.addColumn(row -> row.getInputExpression()).setHeader("Expression value").setSortable(true);
			final TObjectIntMap<String> numNonMappedPerDataset = new TObjectIntHashMap<String>();
			for (final Dataset dataset : datasetsFromDB) {
				grid.addColumn(row -> row.getMappedGene(dataset.getTag()))
						.setHeader("Mapped in DB to (" + dataset.getTag() + ")").setSortable(true);
				grid.addColumn(row -> row.getNumberCellsByDataset(dataset.getTag()))
						.setHeader("Single cells with expression (" + dataset.getTag() + ")").setSortable(true);
				final int numNotMapped = getNumNotMapped(rows, dataset.getTag());
				if (numNotMapped > 0) {
					numNonMappedPerDataset.put(dataset.getTag(), numNotMapped);
				}
			}
			grid.setItems(rows);

			String text = "Data captured from uploaded input file (" + rows.size() + " proteins/genes).";

			if (!numNonMappedPerDataset.isEmpty()) {
				for (final Dataset dataset : datasetsFromDB) {
					if (numNonMappedPerDataset.containsKey(dataset.getTag())) {
						final int numNotMapped = numNonMappedPerDataset.get(dataset.getTag());
						final double percentage = numNotMapped * 1.0 / rows.size();
						text += "\nSome proteins/genes (" + numNotMapped + "/" + rows.size() + " - "
								+ df.format(percentage)
								+ ") from your input list were not found in the database and will be ignored when using dataset ("
								+ dataset.getTag() + ") !!!";
					} else {
						text += "\nAll proteins/genes are mapped in the database for dataset (" + dataset.getTag()
								+ ").";
					}
				}
			} else {
				text += "\nAll proteins/genes are mapped in the database.";
			}
			inputFileDataTabContent.removeAll();
			final Span span = new Span(text);
			span.getElement().getStyle().set("white-space", "pre-wrap");
			inputFileDataTabContent.add(span, grid);
		}
	}

	private final DecimalFormat df = new DecimalFormat("#.#%");

	private int getNumNotMapped(List<MappingRow> rows, String dataset) {
		int ret = 0;
		for (final MappingRow row : rows) {
			final long mapped = row.getNumberCellsByDataset(dataset);
			if (mapped == 0) {
				ret++;
			}
		}
		return ret;
	}

	/**
	 * It reads proteins or genes from input file and makes the mapping to the genes
	 * in the database per each of the datasets available
	 * 
	 * @param inputFile
	 * @return an array in which elements are:<br>
	 *         - index 0: input protein or gene<br>
	 *         - index 1: expression value in the input<br>
	 *         - index 2: mapped gene<br>
	 *         - index 3: number of single cells with some non-zero expression of
	 *         that gene<br>
	 *         the array can have more columns, 2 per different dataset in the
	 *         database
	 * 
	 * @throws Exception
	 */
	private List<MappingRow> validateInputFile(File inputFile) throws Exception {

		final List<MappingRow> rows = new ArrayList<MappingRow>();
		final List<String> genes = new ArrayList<String>();
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
						final String geneName = split[0];
						genes.add(geneName);
						if (split.length < 2) {
							throw new Exception("Second column in missing in input file (line " + numLine + ")");
						}
						try {
							final Double expressionValue = Double.valueOf(split[1]);
							if (!"".equals(geneName)) {
								final MappingRow row = new MappingRow(geneName, expressionValue);
								rows.add(row);
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

		if (rows.isEmpty() && numLine > 0) {
			throw new Exception("Looks like input file is not TAB separated?");
		}
		// now we add the mappings in columns with index 2,3 or more
		final Set<String> datasets = new THashSet<String>();
		datasets.addAll(datasetsFromDB.stream().map(dataset -> dataset.getTag()).collect(Collectors.toList()));

		for (final String dataset : datasets) {
			final Map<String, Pair<String, Long>> mapToDatabase = new InputDataMappingValidation().mapToDatabase(genes,
					null, mbs, dataset);
			// there is a row per gene
			for (int i = 0; i < rows.size(); i++) {
				final MappingRow mappingRow = rows.get(i);
				final String proteinGene = mappingRow.getInputProteinGene();
				// we try to map the input protein or gene to the database for each dataset

				final Pair<String, Long> mapped = mapToDatabase.get(proteinGene);
				if (mapped != null) {
					mappingRow.addMapping(dataset, mapped.getFirstelement(), mapped.getSecondElement());
				} else {
					mappingRow.addMapping(dataset, NOT_MAPPED, 0l);
				}
			}
		}
		return rows;
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
		return new H3("PCTSEA input parameters");
	}

	private Component createFormLayout() {
		final FormLayout formLayout = new FormLayout();
//		email.setErrorMessage("Please enter a valid email address");
		formLayout.add(datasetsCombo);
		formLayout.add(cellTypeBranchCombo);
		formLayout.add(scoringMethodCombo);
		formLayout.add(minCorrelationField);
		formLayout.add(minGenesCellsField);
		formLayout.add(outputPrefixField);
		formLayout.add(numPermutationsField);
		formLayout.add(emailField);
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
				"Tab-separated text file with two columns, the first, containing the protein accessions (UniprotKB) or gene symbols "
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
