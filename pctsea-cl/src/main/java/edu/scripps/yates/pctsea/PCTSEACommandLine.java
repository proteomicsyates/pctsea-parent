package edu.scripps.yates.pctsea;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.correlation.CorrelationThreshold;
import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.swing.CommandLineProgramGuiEnclosable;
import edu.scripps.yates.utilities.swing.DoNotInvokeRunMethod;
import edu.scripps.yates.utilities.swing.SomeErrorInParametersOcurred;
import gnu.trove.set.hash.THashSet;

public class PCTSEACommandLine extends CommandLineProgramGuiEnclosable {
	private final PCTSEA pctsea;
	private DatasetMongoRepository dmr;
	private final Logger log = Logger.getLogger(PCTSEACommandLine.class);
	private String prefix;
	private String email;
	private File experimentExpressionFile;
	private CorrelationThreshold correlationThreshold;
	private int minNumberExpressedGenesInCell;
	private boolean loadRandomDistributionsIfExist;
	private int maxIterations;
	private List<CellTypeBranch> cellTypeBranches;
	private boolean generateCharts;
	private int minCellsPerCellTypeForPDF;
	private boolean plotNegativeEnrichedCellTypes;
	private Set<String> datasets;
	private boolean writeCorrelationsFile;

	public PCTSEACommandLine(String[] args, DatasetMongoRepository dmr, ExpressionMongoRepository emr,
			SingleCellMongoRepository scmr, PctseaRunLogRepository runLog, MongoBaseService mbs)
			throws ParseException, DoNotInvokeRunMethod, SomeErrorInParametersOcurred {
		super(args);
		pctsea = new PCTSEA(emr, scmr, runLog, mbs);
		pctsea.setPrefix(prefix);
		pctsea.setEmail(email);
		pctsea.setExperimentExpressionFile(experimentExpressionFile);
		pctsea.setCorrelationThreshold(correlationThreshold);
		pctsea.setMinNumberExpressedGenesInCell(minNumberExpressedGenesInCell);
		pctsea.setLoadRandomDistributionsIfExist(loadRandomDistributionsIfExist);
		pctsea.setMaxIterations(maxIterations);
		pctsea.setCellTypesBranches(cellTypeBranches);
		pctsea.setGenerateCharts(generateCharts);
		pctsea.setMinCellsPerCellTypeForPDF(minCellsPerCellTypeForPDF);
		pctsea.setPlotNegativeEnrichedCellTypes(plotNegativeEnrichedCellTypes);
		pctsea.setDatasets(datasets);
		pctsea.setWriteCorrelationsFile(writeCorrelationsFile);
	}

	@Override
	public void run() {
		try {
			System.setProperty("java.awt.headless", "true");
			final PCTSEAResult result = pctsea.run();
			log.info("PCTSEA got some results in "
					+ DatesUtil.getDescriptiveTimeFromMillisecs(result.getRunLog().getRunningTime()));
			log.info("Results file created at: " + result.getResultsFile());
			if (result.getUrlToViewer() != null) {
				log.info("Also, results can be visualized at: " + result.getUrlToViewer());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			log.error("Error in PCTSEA:", e);
			log.error("Error message: " + e.getMessage());
			throw e;
		}
	}

	@Override
	public String getTitleForFrame() {
		return "PCTSEA - Proteomic Cell type Set Enrichment Analysis";
	}

	@Override
	protected void initToolFromCommandLineOptions(CommandLine cmd) throws SomeErrorInParametersOcurred {

		if (cmd.hasOption(InputParameters.OUT)) {
			prefix = cmd.getOptionValue(InputParameters.OUT);

		} else {
			errorInParameters("prefix name for the output files is missing");
		}
		if (cmd.hasOption(InputParameters.EMAIL)) {
			email = cmd.getOptionValue(InputParameters.EMAIL);

		} else {
			// we will not be able to send the email with the results
		}
		if (cmd.hasOption(InputParameters.EEF)) {
			experimentExpressionFile = new File(cmd.getOptionValue(InputParameters.EEF));
			final File parentFile = experimentExpressionFile.getParentFile();
			if (parentFile == null || !experimentExpressionFile.exists()) {
				experimentExpressionFile = new File(
						System.getProperty("user.dir") + File.separator + cmd.getOptionValue(InputParameters.EEF));
			}

			if (!experimentExpressionFile.exists()) {
				errorInParameters("experimental_expression_file '-" + InputParameters.EEF + "' '"
						+ cmd.getOptionValue(InputParameters.EEF) + "' doesn't exist or is not found");
			}

		} else {
			errorInParameters("experimental_expression_file is missing");
		}

		correlationThreshold = new CorrelationThreshold(0.1);
		if (cmd.hasOption(InputParameters.MIN_CORRELATION)) {
			try {
				correlationThreshold = new CorrelationThreshold(
						Double.valueOf(cmd.getOptionValue(InputParameters.MIN_CORRELATION)));
				if (correlationThreshold.getThresholdValue() > 1.0 || correlationThreshold.getThresholdValue() < -1.0) {
					throw new IllegalArgumentException();
				}
			} catch (final Exception e) {
				e.printStackTrace();
				errorInParameters("option min_genes_cells '-" + InputParameters.MIN_CORRELATION + "' ('"
						+ cmd.getOptionValue(InputParameters.MIN_CORRELATION)
						+ "') is not valid. Valid values are real numbers between -1 and 1");
			}

		}

		minNumberExpressedGenesInCell = 4;
		if (cmd.hasOption(InputParameters.MIN_GENES_CELLS)) {
			final String mgcString = cmd.getOptionValue(InputParameters.MIN_GENES_CELLS);
			try {
				minNumberExpressedGenesInCell = Integer.valueOf(mgcString);

				if (minNumberExpressedGenesInCell < 0.0) {
					throw new IllegalArgumentException();
				}

			} catch (final Exception e) {
				e.printStackTrace();
				errorInParameters("option '=" + InputParameters.MIN_GENES_CELLS + "' ('"
						+ cmd.getOptionValue(InputParameters.MIN_GENES_CELLS)
						+ "') is not valid. Valid values are positive integers.");
			}

		}

		loadRandomDistributionsIfExist = false;
		if (cmd.hasOption(InputParameters.LOAD_RANDOM)) {
			loadRandomDistributionsIfExist = true;
		}

		maxIterations = 10;
		if (cmd.hasOption(InputParameters.PERM)) {
			try {
				maxIterations = Integer.valueOf(cmd.getOptionValue(InputParameters.PERM));
				if (maxIterations < 0) {
					throw new NumberFormatException();
				}
			} catch (final NumberFormatException e) {
				errorInParameters(
						"Error in value for option '-" + InputParameters.PERM + "'. It must be a positive integer");
			}
		}

		cellTypeBranches = new ArrayList<CellTypeBranch>();

		if (cmd.hasOption(InputParameters.CELL_TYPES_CLASSIFICATION)) {
			final String optionValue = cmd.getOptionValue(InputParameters.CELL_TYPES_CLASSIFICATION);
			try {
				cellTypeBranches.addAll(CellTypeBranch.parseCellTypeBranchesString(optionValue));
			} catch (final Exception e) {
				errorInParameters("Error in value for option '-" + InputParameters.CELL_TYPES_CLASSIFICATION
						+ "'. Possible values (separated by commas are: " + CellTypeBranch.getStringSeparated(",")
						+ ")");
			}
		} else {
			cellTypeBranches.add(CellTypeBranch.TYPE);
		}

		generateCharts = false;
		if (cmd.hasOption(InputParameters.CHARTS)) {
			generateCharts = true;
		}

		minCellsPerCellTypeForPDF = 0;
		if (cmd.hasOption(InputParameters.MIN_CELLS_PER_CELL_TYPE)) {
			try {
				minCellsPerCellTypeForPDF = Integer
						.valueOf(cmd.getOptionValue(InputParameters.MIN_CELLS_PER_CELL_TYPE));
				if (minCellsPerCellTypeForPDF < 0) {
					throw new NumberFormatException();
				}
			} catch (final NumberFormatException e) {
				log.error(e);
				errorInParameters("Error in value for option '-" + InputParameters.MIN_CELLS_PER_CELL_TYPE
						+ "'. It must be a positive integer");
			}
		}

		plotNegativeEnrichedCellTypes = false;
		if (cmd.hasOption(InputParameters.PLOT_NEGATIVE_ENRICHED)) {
			plotNegativeEnrichedCellTypes = true;
		}

		//
		if (cmd.hasOption(InputParameters.DATASETS)) {
			datasets = parseDatasets(cmd.getOptionValue(InputParameters.DATASETS));

		} else {
			// considering to use all datasets
		}
		// write correlations file
		writeCorrelationsFile = true;
		if (cmd.hasOption(InputParameters.WRITE_CORRELATIONS)) {
			try {
				writeCorrelationsFile = Boolean
						.valueOf(cmd.getOptionValue(InputParameters.WRITE_CORRELATIONS).toLowerCase());
			} catch (final Exception e) {
				log.error(e);
				errorInParameters("Error in value for option '-" + InputParameters.WRITE_CORRELATIONS
						+ "'. It must be either true or false.");
			}
		}

	}

	private Set<String> parseDatasets(String datasetsString) {
		final Set<String> ret = new THashSet<String>();
		if (datasetsString.contains(",")) {
			final String[] split = datasetsString.split(",");
			for (final String string : split) {
				ret.add(string.trim());
			}
		} else {
			ret.add(datasetsString.trim());
		}
		return ret;

	}

	@Override
	public String printCommandLineSintax() {
		return "-out [prefix] -eef [experimental expression file]\n";
	}

	@Override
	protected List<Option> defineCommandLineOptions() {
		// create Options object
		final List<Option> options = new ArrayList<Option>();

		final Option option1 = new Option(InputParameters.EEF, true,
				"Path to the file with the expression values of the proteins/genes obtained experimentally and that you want to correlate with their expressions in the single cells.");
		option1.setRequired(true);
		options.add(option1);

//		final Option option3 = new Option("scmf", "single_cells_metadata_file", true,
//				"Path to the file with two columns, the first, the id of the cell and the second the cell type or cluster name.");
//		option3.setRequired(true);
//		options.add(option3);

		final Option option4 = new Option(InputParameters.MIN_CORRELATION, true,
				"Minimum Pearson's correlation to be considered in the cell type enrichment cell. If not provided, it will be 0.1 by default. If negative only the cells with negative correlations below that value will be considered.");
		options.add(option4);

		final Option option5 = new Option(InputParameters.MIN_GENES_CELLS, true,
				"Minimum number of proteins/genes that should have a non-zero expression value in a cell to be considered in the correlation with the experimental data. If not provided it will be 2.");
		options.add(option5);

		final Option option6 = new Option(InputParameters.OUT, true, "Prefix for all output files");
		option6.setRequired(true);
		options.add(option6);

		final Option option7 = new Option(InputParameters.LOAD_RANDOM, false,
				"Load random distributions if there were created before");

		options.add(option7);

		final Option option8 = new Option(InputParameters.PERM, true,
				"Number of permutations for calculating the null distribution of enrichment scores. If not provided, 1000 permutations will be used.");
//		option8.setRequired(true);
		options.add(option8);

		final Option optionCellTypeBranch = new Option(InputParameters.CELL_TYPES_CLASSIFICATION, true,
				"List (separated by commas) of types of cell types classifications according to the hierarchical cell type classification (TYPE, SUBTYPE, CHARACTERISTIC). Possible values of this list are: "
						+ CellTypeBranch.getStringSeparated(",") + ". If not provided, only TYPE will be considered.");
		options.add(optionCellTypeBranch);
		//
		final Option optionGenerateCharts = new Option(InputParameters.CHARTS, false,
				"Generate a PDF file with the charts images with the enrichment calculations and the correlations distributions. If not selected, the following options will be ignored.");

		options.add(optionGenerateCharts);

		//
		final Option optionMinCellTypeVariance = new Option(InputParameters.MIN_CELLS_PER_CELL_TYPE, true,
				"Minimum number of cells per cell type that pass the correlation threshold to be included in the output PDF report.");
		options.add(optionMinCellTypeVariance);
		//
		final Option optionOnlyPlotPositiveEnrichedCellTypes = new Option(InputParameters.PLOT_NEGATIVE_ENRICHED, false,
				"If present, plots associated with cell types with negative enrichment scores will be included in the output PDF report.");
		options.add(optionOnlyPlotPositiveEnrichedCellTypes);
		//
		final Option email = new Option(InputParameters.EMAIL, true,
				"If the system supports it, a link to view the results will be sent to the email address provided.");
		options.add(email);
		//
		final Option datasets = new Option(InputParameters.DATASETS, true,
				"Comma separated values of the dataset against you want to analyze your data.");
		options.add(datasets);

		final Option writeCorrelationsFileOpton = new Option(InputParameters.WRITE_CORRELATIONS, true,
				"Whether to write or not a file with the correlation values between all single cells and the input data. Default value if not provided: true.");
		options.add(writeCorrelationsFileOpton);

		return options;
	}

	private String getAvailableDatasets() {
		final StringBuilder sb = new StringBuilder();

		if (dmr != null) {
			final List<Dataset> findAll = dmr.findAll();
			for (final Dataset dataset : findAll) {
				if (!"".equals(sb.toString())) {
					sb.append(",");
				}
				sb.append(dataset.getTag());
			}
		}
		return sb.toString();
	}
}
