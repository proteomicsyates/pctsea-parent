package edu.scripps.yates.pctsea;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import edu.scripps.yates.pctsea.correlation.CorrelationThreshold;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.utilities.swing.CommandLineProgramGuiEnclosable;
import edu.scripps.yates.utilities.swing.DoNotInvokeRunMethod;
import edu.scripps.yates.utilities.swing.SomeErrorInParametersOcurred;

public class PCTSEACommandLine extends CommandLineProgramGuiEnclosable {
	private final PCTSEA pctsea;

	public PCTSEACommandLine(String[] args, ExpressionMongoRepository emr, SingleCellMongoRepository scmr)
			throws ParseException, DoNotInvokeRunMethod, SomeErrorInParametersOcurred {
		super(args);
		pctsea = new PCTSEA(emr, scmr);
	}

	@Override
	public void run() {
		this.pctsea.run();
	}

	@Override
	public String getTitleForFrame() {
		return "PCTSEA - Proteomic Cell type Set Enrichment Analysis";
	}

	@Override
	protected void initToolFromCommandLineOptions(CommandLine cmd) throws SomeErrorInParametersOcurred {

		if (cmd.hasOption(InputParameters.OUT)) {
			this.pctsea.setPrefix(cmd.getOptionValue(InputParameters.OUT));

		} else {
			errorInParameters("prefix name for the output files is missing");
		}
		if (cmd.hasOption(InputParameters.EEF)) {
			File experimentExpressionFile = new File(cmd.getOptionValue(InputParameters.EEF));
			final File parentFile = experimentExpressionFile.getParentFile();
			if (parentFile == null || !experimentExpressionFile.exists()) {
				experimentExpressionFile = new File(
						System.getProperty("user.dir") + File.separator + cmd.getOptionValue(InputParameters.EEF));
			}

			if (!experimentExpressionFile.exists()) {
				errorInParameters("experimental_expression_file '-" + InputParameters.EEF + "' '"
						+ cmd.getOptionValue(InputParameters.EEF) + "' doesn't exist or is not found");
			}
			pctsea.setExperimentExpressionFile(experimentExpressionFile);
		} else {
			errorInParameters("experimental_expression_file is missing");
		}

		CorrelationThreshold correlationThreshold = new CorrelationThreshold(0.1);
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
		pctsea.setCorrelationThreshold(correlationThreshold);
		double minNumberExpressedGenesInCell = 2.0;
		if (cmd.hasOption(InputParameters.MIN_GENES_CELLS)) {
			final String mgcString = cmd.getOptionValue(InputParameters.MIN_GENES_CELLS);
			try {
				minNumberExpressedGenesInCell = Double.valueOf(mgcString);

				if (minNumberExpressedGenesInCell < 0.0) {
					throw new IllegalArgumentException();
				}

			} catch (final Exception e) {
				e.printStackTrace();
				errorInParameters("option '=" + InputParameters.MIN_GENES_CELLS + "' ('"
						+ cmd.getOptionValue(InputParameters.MIN_GENES_CELLS)
						+ "') is not valid. Valid values are positive integers or a real number between 0 and 1 representing a percentage (i.e. 0.1 will correspond to 10%)");
			}

		}
		pctsea.setMinNumberExpressedGenesInCell(minNumberExpressedGenesInCell);

		boolean loadRandomDistributionsIfExist = false;
		if (cmd.hasOption(InputParameters.LOAD_RANDOM)) {
			loadRandomDistributionsIfExist = true;
		}
		pctsea.setLoadRandomDistributionsIfExist(loadRandomDistributionsIfExist);

		int maxIterations = 1000;
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
		pctsea.setMaxIterations(maxIterations);

		final List<CellTypeBranch> cellTypeBranches = new ArrayList<CellTypeBranch>();

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
		pctsea.setCellTypesBranches(cellTypeBranches);

		boolean generateCharts = false;
		if (cmd.hasOption(InputParameters.CHARTS)) {
			generateCharts = true;
		}
		pctsea.setGenerateCharts(generateCharts);

		int minCellsPerCellTypeForPDF = 0;
		if (cmd.hasOption(InputParameters.MIN_CELLS_PER_CELL_TYPE)) {
			try {
				minCellsPerCellTypeForPDF = Integer
						.valueOf(cmd.getOptionValue(InputParameters.MIN_CELLS_PER_CELL_TYPE));
				if (minCellsPerCellTypeForPDF < 0) {
					throw new NumberFormatException();
				}
			} catch (final NumberFormatException e) {
				errorInParameters(
						"Error in value for option '-min_cells_per_cell_type'. It must be a positive integer");
			}
		}
		pctsea.setMinCellsPerCellTypeForPDF(minCellsPerCellTypeForPDF);

		boolean plotNegativeEnrichedCellTypes = false;
		if (cmd.hasOption(InputParameters.PLOT_NEGATIVE_ENRICHED)) {
			plotNegativeEnrichedCellTypes = true;
		}
		pctsea.setPlotNegativeEnrichedCellTypes(plotNegativeEnrichedCellTypes);
	}

	@Override
	public String printCommandLineSintax() {
		return "-out [prefix] -eef [experimental expression file]";
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
		return options;
	}
}
