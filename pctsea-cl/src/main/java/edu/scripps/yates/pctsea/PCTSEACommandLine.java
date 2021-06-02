package edu.scripps.yates.pctsea;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.db.CellTypeAndGeneMongoRepository;
import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.InputDataType;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.pctsea.model.ScoringMethod;
import edu.scripps.yates.pctsea.model.ScoringSchema;
import edu.scripps.yates.pctsea.scoring.NoThreshold;
import edu.scripps.yates.pctsea.scoring.ScoreThreshold;
import edu.scripps.yates.utilities.swing.CommandLineProgramGuiEnclosable;
import edu.scripps.yates.utilities.swing.DoNotInvokeRunMethod;
import edu.scripps.yates.utilities.swing.SomeErrorInParametersOcurred;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

public class PCTSEACommandLine extends CommandLineProgramGuiEnclosable {
	private final PCTSEA pctsea;
	private DatasetMongoRepository dmr;
	private final Logger log = Logger.getLogger(PCTSEACommandLine.class);
	private String prefix;
	private String email;
	private File experimentExpressionFile;
	private TDoubleList scoresPerRound;
	private TIntList minNumberExpressedGenesInCellPerRound;
	private boolean loadRandomDistributionsIfExist;
	private int maxIterations;
	private CellTypeBranch cellTypeBranch;
	private boolean plotNegativeEnrichedCellTypes;
	private Set<Dataset> datasets;
	private boolean writeCorrelationsFile;
	private String uniprotRelease;
	private List<ScoringMethod> scoringMethodsPerRound;
	private InputDataType inputDataType;

	public PCTSEACommandLine(String[] args, DatasetMongoRepository dmr, ExpressionMongoRepository emr,
			SingleCellMongoRepository scmr, PctseaRunLogRepository runLog, CellTypeAndGeneMongoRepository ctgmr,
			MongoBaseService mbs) throws ParseException, DoNotInvokeRunMethod, SomeErrorInParametersOcurred {
		super(args);
		pctsea = new PCTSEA(emr, scmr, runLog, dmr, ctgmr, mbs);

	}

	@Override
	public void run() {
		try {
			System.setProperty("java.awt.headless", "true");
			pctsea.setPrefix(prefix);
			pctsea.setEmail(email);
			pctsea.setExperimentExpressionFile(experimentExpressionFile);
			for (int i = 0; i < scoringMethodsPerRound.size(); i++) {

				final ScoringMethod scoringMethod = scoringMethodsPerRound.get(i);
				final int minNumberExpressedGenesInCell = minNumberExpressedGenesInCellPerRound.get(i);
				if (scoringMethod == ScoringMethod.QUICK_SCORE) {
					pctsea.addScoreSchema(
							new ScoringSchema(scoringMethod, new NoThreshold(), minNumberExpressedGenesInCell));
				} else {
					pctsea.addScoreSchema(new ScoringSchema(scoringMethod, new ScoreThreshold(scoresPerRound.get(i)),
							minNumberExpressedGenesInCell));
				}
			}

			pctsea.setLoadRandomDistributionsIfExist(loadRandomDistributionsIfExist);
			pctsea.setMaxIterations(maxIterations);
			pctsea.setCellTypesBranch(cellTypeBranch);
			pctsea.setPlotNegativeEnrichedCellTypes(plotNegativeEnrichedCellTypes);
			pctsea.setDatasets(datasets);
			pctsea.setWriteCorrelationsFile(writeCorrelationsFile);
			pctsea.setUniprotRelease(uniprotRelease);

			pctsea.setInputDataType(inputDataType);
			// to make log go to the textarea when calling to the status listener
			pctsea.setStatusListener(this);
			final PCTSEAResult result = pctsea.run();

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
		scoresPerRound = new TDoubleArrayList();
		if (cmd.hasOption(InputParameters.MIN_SCORE)) {
			String minScoreString = null;
			try {
				minScoreString = cmd.getOptionValue(InputParameters.MIN_SCORE);
				if (minScoreString.contains(",")) {
					final String[] split = minScoreString.split(",");
					for (final String string : split) {

						scoresPerRound.add(Double.valueOf(string));

					}
				} else {
					scoresPerRound.add(Double.valueOf(minScoreString));
				}

			} catch (final Exception e) {
				e.printStackTrace();
				errorInParameters("option '" + InputParameters.MIN_SCORE + "' ('" + minScoreString
						+ "') is not valid. Valid values are real numbers");
			}
		} else {
			scoresPerRound.add(0.1);
		}

		minNumberExpressedGenesInCellPerRound = new TIntArrayList();
		if (cmd.hasOption(InputParameters.MIN_GENES_CELLS)) {
			final String mgcString = cmd.getOptionValue(InputParameters.MIN_GENES_CELLS);
			try {
				if (mgcString.contains(",")) {
					final String[] split = mgcString.split(",");
					for (final String string : split) {
						final int minNumberExpressedGenesInCellNumber = Integer.valueOf(string);
						minNumberExpressedGenesInCellPerRound.add(minNumberExpressedGenesInCellNumber);
					}
				} else {
					final int minNumberExpressedGenesInCellNumber = Integer.valueOf(mgcString);
					minNumberExpressedGenesInCellPerRound.add(minNumberExpressedGenesInCellNumber);
				}

			} catch (final Exception e) {
				e.printStackTrace();
				errorInParameters("option '" + InputParameters.MIN_GENES_CELLS + "' ('"
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

		cellTypeBranch = CellTypeBranch.ORIGINAL;

		if (cmd.hasOption(InputParameters.CELL_TYPES_CLASSIFICATION)) {
			final String optionValue = cmd.getOptionValue(InputParameters.CELL_TYPES_CLASSIFICATION).trim();
			try {
				cellTypeBranch = CellTypeBranch.valueOf(optionValue);
				if (cellTypeBranch == null) {
					throw new Exception("");
				}
			} catch (final Exception e) {
				errorInParameters("Error in value for option '-" + InputParameters.CELL_TYPES_CLASSIFICATION
						+ "'. Possible values are: " + CellTypeBranch.getStringSeparated(","));
			}
		}

		plotNegativeEnrichedCellTypes = false;
		if (cmd.hasOption(InputParameters.PLOT_NEGATIVE_ENRICHED)) {
			plotNegativeEnrichedCellTypes = true;
		}

		//

		if (cmd.hasOption(InputParameters.DATASETS)) {
			datasets = new HashSet<Dataset>();
			final String optionValue = cmd.getOptionValue(InputParameters.DATASETS).trim();
			if (optionValue.contains(",")) {
				final String[] split = optionValue.split(",");
				for (final String datasetTag : split) {
					datasets.add(new Dataset(datasetTag.trim(), datasetTag.trim(), null));
				}
			} else {
				datasets.add(new Dataset(optionValue, optionValue, null));
			}
		} else {
			// considering to use all datasets
		}
		// write correlations file
		writeCorrelationsFile = false;
		if (cmd.hasOption(InputParameters.WRITE_SCORES)) {
			writeCorrelationsFile = true;
		}
		uniprotRelease = null;
		if (cmd.hasOption(InputParameters.UNIPROT_RELEASE)) {
			uniprotRelease = cmd.getOptionValue(InputParameters.UNIPROT_RELEASE).trim();
		}

		//
		scoringMethodsPerRound = new ArrayList<ScoringMethod>();
		if (cmd.hasOption(InputParameters.SCORING_METHOD)) {
			final String scoringMethodString = cmd.getOptionValue(InputParameters.SCORING_METHOD);
			try {
				if (scoringMethodString.contains(",")) {
					final String[] split = scoringMethodString.split(",");
					for (final String string : split) {
						final ScoringMethod scoringMethod = ScoringMethod.getByScoreName(string.trim());
						scoringMethodsPerRound.add(scoringMethod);
					}
				} else {
					final ScoringMethod scoringMethod = ScoringMethod.getByScoreName(scoringMethodString.trim());
					scoringMethodsPerRound.add(scoringMethod);
				}

			} catch (final Exception e) {
				errorInParameters("Error in value for option '-" + InputParameters.SCORING_METHOD + "'. Value '"
						+ scoringMethodString.trim() + "' not recognized. Possible values are: "
						+ ScoringMethod.getStringSeparated(","));
			}
		} else {
			errorInParameters("option '" + InputParameters.SCORING_METHOD
					+ " is missing. Valid values are real numbers between -1 and 1");
		}
		//
		if (cmd.hasOption(InputParameters.INPUT_DATA_TYPE))

		{
			try {
				inputDataType = InputDataType.valueOf(cmd.getOptionValue(InputParameters.INPUT_DATA_TYPE).trim());
			} catch (final Exception e) {
				errorInParameters("Error in value for option '-" + InputParameters.INPUT_DATA_TYPE + "'. Value '"
						+ cmd.getOptionValue(InputParameters.INPUT_DATA_TYPE).trim()
						+ "' not recognized. Possible values are: " + InputDataType.getStringSeparated(","));
			}
		}

		// check the number of parameters regarding the scoring schema
		final boolean valid = minNumberExpressedGenesInCellPerRound.size() == scoresPerRound.size()
				&& scoresPerRound.size() == scoringMethodsPerRound.size();
		if (!valid) {
			errorInParameters(
					"The number of parameters (separated by commas if more than one) of the following parameters must be the same:"
							+ InputParameters.MIN_GENES_CELLS + "-" + InputParameters.MIN_SCORE + "_"
							+ InputParameters.SCORING_METHOD);
		}
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

		final Option option4 = new Option(InputParameters.MIN_SCORE, true,
				"Minimum Score to be considered in the cell type enrichment cell. If several "
						+ InputParameters.SCORING_METHOD
						+ " are provided, several values separated by commas must be provided for this parameter");
		options.add(option4);
		option4.setRequired(true);

		final Option option5 = new Option(InputParameters.MIN_GENES_CELLS, true,
				"Minimum number of proteins/genes that should have a non-zero expression value in a cell to be considered in the corresponding scoring."
						+ " If several " + InputParameters.SCORING_METHOD
						+ " are provided, several values separated by commas must be provided for this parameter");
		option5.setRequired(true);
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
				"Level of cell type classification according to the hierarchical structure of its classification. Consult the administrator to know more about it. Possible values of this list are: "
						+ CellTypeBranch.getStringSeparated(",") + ". If not provided, "
						+ CellTypeBranch.ORIGINAL.name() + " will be considered.");
		options.add(optionCellTypeBranch);

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
				"Comma separated values of the datasets against you want to analyze your data. If empty, all datasets will be used.");
//		datasets.setRequired(true);
		options.add(datasets);

		final Option writeCorrelationsFileOpton = new Option(InputParameters.WRITE_SCORES, false,
				"Whether to write or not a file with the scores associated to each single cell used to rank them for the KS-statistics. Default value if not provided: False.");
		options.add(writeCorrelationsFileOpton);

		final Option uniprotReleaseOpton = new Option(InputParameters.UNIPROT_RELEASE, true,
				"Uniprot release to use as stated at ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/reldate.txt.");
		options.add(uniprotReleaseOpton);

		final Option scoringMethodOption = new Option(InputParameters.SCORING_METHOD, true,
				"Scoring method used in the algorithm. Possible values are: " + ScoringMethod.getStringSeparated(",")
						+ ". Value if not provided: " + ScoringMethod.PEARSONS_CORRELATION
						+ ". Multiple values can be provided if separated by commas and a minimum score should be provided for each one with "
						+ InputParameters.MIN_SCORE + " parameter.");
		scoringMethodOption.setRequired(true);
		options.add(scoringMethodOption);

		final Option inputDataTypeOption = new Option(InputParameters.INPUT_DATA_TYPE, true,
				"Type of input protein/gene list. Possible values are: " + InputDataType.getStringSeparated(","));
		inputDataTypeOption.setRequired(true);
		options.add(inputDataTypeOption);

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
