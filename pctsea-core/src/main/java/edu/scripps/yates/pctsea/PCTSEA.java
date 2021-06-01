package edu.scripps.yates.pctsea;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

import com.google.common.io.Files;

import edu.scripps.yates.pctsea.db.CellTypeAndGeneMongoRepository;
import edu.scripps.yates.pctsea.db.CellTypeAndGenesDBUtil;
import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLog;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.model.CellTypes;
import edu.scripps.yates.pctsea.model.Gene;
import edu.scripps.yates.pctsea.model.GeneOccurrence;
import edu.scripps.yates.pctsea.model.InputDataType;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.pctsea.model.ScoringMethod;
import edu.scripps.yates.pctsea.model.ScoringSchema;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.model.charts.LabelGenerator;
import edu.scripps.yates.pctsea.model.charts.LabeledXYDataset;
import edu.scripps.yates.pctsea.scoring.NoThreshold;
import edu.scripps.yates.pctsea.scoring.ScoreThreshold;
import edu.scripps.yates.pctsea.utils.CellTypesOutputTableColumns;
import edu.scripps.yates.pctsea.utils.EmailUtil;
import edu.scripps.yates.pctsea.utils.PCTSEAUtils;
import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import edu.scripps.yates.pctsea.utils.parallel.EnrichmentWeigthedScoreParallel;
import edu.scripps.yates.utilities.appversion.AppVersion;
import edu.scripps.yates.utilities.cores.SystemCoreManager;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.files.ZipManager;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.maths.PValueCorrection;
import edu.scripps.yates.utilities.maths.PValueCorrectionType;
import edu.scripps.yates.utilities.pi.ConcurrentUtil;
import edu.scripps.yates.utilities.pi.ParIterator;
import edu.scripps.yates.utilities.pi.ParIterator.Schedule;
import edu.scripps.yates.utilities.pi.ParIteratorFactory;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import edu.scripps.yates.utilities.strings.StringUtils;
import edu.scripps.yates.utilities.swing.AutomaticGUICreator;
import edu.scripps.yates.utilities.swing.StatusListener;
import gnu.trove.iterator.TFloatIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;
import tagbio.umap.Umap;
import tagbio.umap.metric.EuclideanMetric;

public class PCTSEA {

	private final ExpressionMongoRepository expressionMongoRepo;
	private final DatasetMongoRepository datasetMongoRepo;
	private final SingleCellMongoRepository singleCellMongoRepo;
	private final PctseaRunLogRepository runLogsRepo;
	private final CellTypeAndGeneMongoRepository cellTypeGeneRepo;
	private final MongoBaseService mongoBaseService;

	private final static org.slf4j.Logger log = LoggerFactory.getLogger(PCTSEA.class);
//	private final static File filesfolder = new File(
//			"C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human");
//	private final static String mProteinsExpressionsFileName = "mprotein_fullTable.txt";
//	private final static String sProteinsExpressionsFileName = "spike_fullTable.txt";
//	private final static String singleCellInformationFileName = "singleCellLinesTable_fullTable.txt";
//	private final static String mProteinsCorrelationsFileName = "mprotein_singleCells.txt";
//	private final static String sProteinsCorrelationsFileName = "spike_singleCells.txt";

	private File experimentExpressionFile;
//	private File singleCellsMetadataFile;
	private final boolean takeZerosForCorrelation = false;
	private String prefix;

	private int maxIterations;
	private boolean loadRandomDistributionsIfExist;
	private static final int threadCount = SystemCoreManager.getAvailableNumSystemCores() * 2;
	private static final int MIN_CELLS_PASSING_SCORE_TRESHOLD = 1000;
	private CellTypeBranch cellTypeBranch;
	private InteractorsExpressionsRetriever interactorExpressions;
	protected Future<?> savingFiles;
	private boolean plotNegativeEnrichedCellTypes;

	private static StatusListener<Boolean> statusListener;

	private String currentTimeStamp;

	private String email;

	private Dataset dataset;

	private boolean writeScoresFile = false;

	private String fromEmail;
	private String uniprotRelease;
	/**
	 * Sequentially applied scoring methods
	 */
	private final List<ScoringSchema> sequentialScoringSchemas = new ArrayList<ScoringSchema>();
	private InputDataType inputDataType;
	private boolean currentTimeStampFolderCreated;
	private File resultsSubFolderForCellTypes;
	private File resultsSubFolderGeneral;
	private String resultsViewerURL;
	private static final double CELL_TYPE_FDR_SIGNIFICANCE_THRESHOLD = 0.05;
	private static final int MAX_SINGLE_CELLS_FOR_SCORE = 60000;

	public PCTSEA(InputParameters inputParameters, ExpressionMongoRepository expressionMongoRepo,
			SingleCellMongoRepository singleCellMongoRepo, PctseaRunLogRepository runLogsRepo,
			DatasetMongoRepository datasetMongoRepo, CellTypeAndGeneMongoRepository cellTypeGeneRepo,
			MongoBaseService mongoBaseService) {

		this.expressionMongoRepo = expressionMongoRepo;
		this.singleCellMongoRepo = singleCellMongoRepo;
		this.mongoBaseService = mongoBaseService;
		this.datasetMongoRepo = datasetMongoRepo;
		this.cellTypeGeneRepo = cellTypeGeneRepo;
		this.runLogsRepo = runLogsRepo;

		cellTypeBranch = inputParameters.getCellTypeBranch();

		sequentialScoringSchemas.addAll(inputParameters.getScoringSchemas());

		experimentExpressionFile = new File(inputParameters.getInputDataFile());
		loadRandomDistributionsIfExist = inputParameters.isLoadRandom();
		maxIterations = inputParameters.getNumPermutations();
		writeScoresFile = inputParameters.isWriteScoresFile();
		email = inputParameters.getEmail();
		dataset = inputParameters.getDataset();
		uniprotRelease = inputParameters.getUniprotRelease();
		inputDataType = inputParameters.getInputDataType();
		// we check validity of prefix as file name
		if (inputParameters.getOutputPrefix() != null) {
			prefix = FileUtils.checkInvalidCharacterNameForFileName(inputParameters.getOutputPrefix());
			if (!prefix.equals(inputParameters.getOutputPrefix())) {
				throw new IllegalArgumentException("Prefix contains invalid characters");
			}
		}

	}

	public PCTSEA(ExpressionMongoRepository expressionMongoRepo, SingleCellMongoRepository singleCellMongoRepo,
			PctseaRunLogRepository runLogsRepo, DatasetMongoRepository datasetMongoRepo,
			CellTypeAndGeneMongoRepository cellTypeAndGeneMongoRepo, MongoBaseService mongoBaseService) {
		this.expressionMongoRepo = expressionMongoRepo;
		this.singleCellMongoRepo = singleCellMongoRepo;
		this.mongoBaseService = mongoBaseService;
		this.runLogsRepo = runLogsRepo;
		this.datasetMongoRepo = datasetMongoRepo;
		cellTypeGeneRepo = cellTypeAndGeneMongoRepo;
	}

	public String getResultsViewerURL() {
		return resultsViewerURL;
	}

	public void setResultsViewerURL(String resultsViewerURL) {

		try {
			this.resultsViewerURL = new URL(resultsViewerURL).toURI().toString();

			if (resultsViewerURL.endsWith("/")) {
				this.resultsViewerURL = this.resultsViewerURL.substring(0, this.resultsViewerURL.length() - 1);
			}
		} catch (MalformedURLException | URISyntaxException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param args
	 */
//	public static void main(String[] args) {
//
//		PCTSEA c = null;
//		try {
//			c = new PCTSEA(args);
//
//			c.safeRun();
//
//		} catch (final DoNotInvokeRunMethod e) {
//			// do nothing
//		} catch (final Exception e) {
//			e.printStackTrace();
//		}
//
//	}

	public PCTSEAResult run() {
		logStatus("Before starting...");
		logInputParams(getInputParameters());
		logStatus("Starting...");

		// check dataset input parameters
		List<Dataset> datasetFromDB = null;
		if (getInputParameters().getDataset() != null) {
			final String tag = getInputParameters().getDataset().getTag();
			logStatus("Looking for dataset in DB with tag '" + tag + "'...");

			datasetFromDB = datasetMongoRepo.findByTag(tag);

			if (datasetFromDB == null || datasetFromDB.isEmpty()) {
				logStatus("Datasets in DB with tag'" + tag + "' are not found");
				final List<Dataset> datasets = datasetMongoRepo.findAll();
				final List<String> datasetTags = datasets.stream().map(dataset -> dataset.getTag()).sorted()
						.collect(Collectors.toList());
				final String datasetsString = StringUtils.getSortedSeparatedValueStringFromChars(datasetTags, ",");
				throw new IllegalArgumentException(
						"Dataset " + tag + " doesn't exist in DB. Available datasets are: " + datasetsString);
			} else {
				logStatus(datasetFromDB.get(0).getTag() + " dataset found in DB with tag'" + tag);
			}
		} else {
			final List<String> datasetTags = datasetMongoRepo.findAll().stream().map(dataset -> dataset.getTag())
					.sorted().collect(Collectors.toList());
			final String datasetsString = StringUtils.getSortedSeparatedValueStringFromChars(datasetTags, ",");
			throw new IllegalArgumentException(
					"Dataset is not specified! Use '-datasets' option with any of these values: " + datasetsString);
		}

		// first of all create a time stamp
		currentTimeStamp = createTimeStamp(prefix);
		currentTimeStampFolderCreated = false;
		// create log
		final PctseaRunLog runLog = new PctseaRunLog();
		runLog.setTimeStamp(currentTimeStamp);
		runLog.setStarted(getDateNow());
		runLog.setInputParameters(getInputParameters());
		// save to repo
		runLogsRepo.save(runLog);

		Exception errorMessage = null;

		// create PCTSEAResult object
		PCTSEAResult result = null;
		try {

			result = new PCTSEAResult(runLog);

//			// read input files
//			final SingleCellsMetaInformationReader cellsInfo = new SingleCellsMetaInformationReader(
//					this.singleCellsMetadataFile);
//			final List<SingleCell> singleCellList = cellsInfo.getSingleCellList();
//			updateSingleCellsType(cellsInfo, this.expressionMongoRepo);
//			if (true) {
//				return;
//			}
			final int numGenes = (int) java.nio.file.Files.readAllLines(experimentExpressionFile.toPath()).stream()
					.filter(f -> !"".equals(f.trim())).count();
			final List<SingleCell> singleCellList = getSingleCellListFromDB(dataset, cellTypeBranch, numGenes);

			interactorExpressions = new InteractorsExpressionsRetriever(mongoBaseService, experimentExpressionFile,
					dataset, uniprotRelease, runLog, singleCellList);

			// log
			runLog.setNumInputGenes(interactorExpressions.getInteractorsGeneIDs().size());

			log.info(singleCellList.size() + " cells");

			// here we store the cell types from each round
			final List<List<CellTypeClassification>> cellTypesPerRound = new ArrayList<List<CellTypeClassification>>();
			for (final ScoringSchema scoringSchema : sequentialScoringSchemas) {
				final ScoringMethod scoringMethod = scoringSchema.getScoringMethod();
				final File zipOutputFile = getZipOutputFile(scoringMethod);
				result.addResultsFile(zipOutputFile);
				if (resultsViewerURL != null) {
					final URL urlToViewer = new URL(resultsViewerURL + "/?results="
							+ FilenameUtils.getBaseName(zipOutputFile.getAbsolutePath()));
					result.addUrlToViewer(urlToViewer);
					log.debug("Setting results URL at: " + urlToViewer.toString());
				}
				final ScoreThreshold scoreThreshold = scoringSchema.getScoringThreshold();
				if (scoringMethod != ScoringMethod.QUICK_SCORE) {

					// calculate correlations
					final int numCellsPassingScoreThreshold = calculateScoresToRankSingleCells(singleCellList,
							interactorExpressions, scoringSchema, writeScoresFile, true, true, takeZerosForCorrelation);
					// log
					runLog.setNumCellsPassingScoreThreshold(numCellsPassingScoreThreshold);
					// note that we still have all single cells in singleCellList also with the ones
					// that dont pass the threshold

					if (numCellsPassingScoreThreshold < MIN_CELLS_PASSING_SCORE_TRESHOLD) {
						throw new IllegalArgumentException(
								"There is not enough cells passing the " + scoringMethod.getScoreName() + " threshold:"
										+ scoreThreshold + " (minimum is " + MIN_CELLS_PASSING_SCORE_TRESHOLD + ")");
					}

					// discard single cells that have negative correlation if scoring is correlation
					if (scoringMethod == ScoringMethod.PEARSONS_CORRELATION) {
						final Iterator<SingleCell> iterator = singleCellList.iterator();
						while (iterator.hasNext()) {

							final SingleCell cell = iterator.next();
							if (cell.getScoreForRanking() < 0) {
								iterator.remove();
							}
						}
						ConcurrentUtil.sleep(1L);
						PCTSEA.logStatus(singleCellList.size() + " single cells have positive correlations (> 0)");
					}
					// make a chart with the histogram of number of genes used to correlate for each
					// cells with and without the threshold
					createHistogramOfCorrelatingGenes(singleCellList, null, scoringMethod);
					createHistogramOfCorrelatingGenes(singleCellList, scoreThreshold.getThresholdValue(),
							scoringMethod);
					// make a chart with the distribution of correlations over the ranked list of
					// cells
					if (scoringMethod == ScoringMethod.PEARSONS_CORRELATION) {
						createDistributionChartOfCorrelationsOverRankedCells(singleCellList, scoringSchema);
					} else {
						createDistributionChartOfScoresOverRankedCells(singleCellList, scoringSchema);
					}
					ConcurrentUtil.sleep(1L);
				}
				// calculate hypergeometric statistics
				// this creates the cell types that are part of the output
				final List<CellTypeClassification> cellTypeClassificationsInRound = calculateHyperGeometricStatistics(
						singleCellList, scoreThreshold, scoringMethod);

				// calculate enrichment scores with the Kolmogorov-Smirnov test
				List<SingleCell> singleCellsPassingScoreThreshold = scoreThreshold
						.getSingleCellsPassingThresholdSortedByScore(singleCellList);
				singleCellsPassingScoreThreshold = singleCellsPassingScoreThreshold.subList(0,
						Math.min(singleCellsPassingScoreThreshold.size() - 1, MAX_SINGLE_CELLS_FOR_SCORE));
				if (scoringMethod != ScoringMethod.QUICK_SCORE) {
					calculateEnrichmentScore(cellTypeClassificationsInRound, singleCellsPassingScoreThreshold, true,
							true, false, plotNegativeEnrichedCellTypes, scoringMethod);

					// calculate significance by cell types permutations
					calculateSignificanceByCellTypesPermutations(interactorExpressions, cellTypeClassificationsInRound,
							singleCellsPassingScoreThreshold, cellTypeBranch, maxIterations,
							loadRandomDistributionsIfExist, plotNegativeEnrichedCellTypes, scoringMethod);

					// calculate significance by phenotype permutations
					if (false) { // DISABLED since we used significance by cell types permutations
						calculateSignificanceByPhenotypePermutations(interactorExpressions,
								cellTypeClassificationsInRound, singleCellsPassingScoreThreshold, cellTypeBranch,
								scoringSchema, loadRandomDistributionsIfExist, plotNegativeEnrichedCellTypes,
								maxIterations, takeZerosForCorrelation);
					}
				} else {
					// calculate quick score
					calculateQuickScore(cellTypeClassificationsInRound, scoreThreshold);
				}
				cellTypesPerRound.add(cellTypeClassificationsInRound);

				// perform the post analysis with the significant cell types
				postAnalysis(cellTypeClassificationsInRound, singleCellList.size());

				// perform a clustering of the genes participating in each cell type
				umapClustering(cellTypeClassificationsInRound, scoringSchema);

				// with no filtering

				// export to output: Prints cell type classifications into a table in a file
				printCellTypeClassifications(cellTypeClassificationsInRound, singleCellList, scoringSchema);

				// plots about suprema
				createScatterPlotOfSuprema(cellTypeClassificationsInRound, plotNegativeEnrichedCellTypes,
						scoringMethod);
				createHistogramOfSuprema(cellTypeClassificationsInRound, plotNegativeEnrichedCellTypes, scoringMethod);
				// print mapping of cell types
//					SingleCell.printCellTypeMapping(getCellTypesMappingOutputFile());
				// export file with genes involved in the correlations per cell type
				printGenesInvolvedInScores(cellTypeClassificationsInRound, scoringSchema);

				writeGZipOutputFile(zipOutputFile, scoringSchema);
				currentTimeStampFolderCreated = false;
			}
			// now we get the cell types that are significant in all rounds
			final List<CellTypeClassification> cellTypeClassifications = getIntersection(cellTypesPerRound);

			return result;
		} catch (final IOException e) {
			errorMessage = e;
			throw new RuntimeException(e);
		} catch (final RuntimeException e) {
			errorMessage = e;
			throw e;
		} finally {
			if (errorMessage != null) {
				if (savingFiles != null && !savingFiles.isDone()) {
					savingFiles.cancel(true);
				}

			} else {
				if (savingFiles != null) {
					if (!savingFiles.isDone()) {

						logStatus("Waiting for thread creating charts to finish...");
					}
					try {
						savingFiles.get();
					} catch (final InterruptedException e1) {
						logStatus("Thread creating charts has been interrupted: " + e1.getMessage(), LogLevel.ERROR);
					} catch (final ExecutionException e1) {
						logStatus("Thread creating charts got an error: " + e1.getMessage(), LogLevel.ERROR);
					}
				}

				// set finish time
				runLog.setFinished(getDateNow());
				// update log
				runLogsRepo.save(runLog);

				if (runLog.getInputParameters().getEmail() != null && fromEmail != null) {
					EmailUtil.sendEmailWithResults(result, fromEmail);
				}
				log.info("PCTSEA got some results in "
						+ DatesUtil.getDescriptiveTimeFromMillisecs(result.getRunLog().getRunningTime()));
				for (int i = 0; i < result.getResultsFiles().size(); i++) {
					log.info("Results file created at: " + result.getResultsFiles().get(i));
					if (result.getUrlToViewers() != null && result.getUrlToViewers().size() > i
							&& result.getUrlToViewers().get(i) != null) {
						log.info("Also, results can be visualized at: " + result.getUrlToViewers().get(i));
					}
				}

				logStatus("Finishing now.");
			}
		}
	}

	private List<CellTypeClassification> getIntersection(List<List<CellTypeClassification>> cellTypesPerRound) {
		final List<CellTypeClassification> validCellTypes = new ArrayList<CellTypeClassification>();
		final Set<String> cellTypeNames = new THashSet<String>();
		// first I store the names of the first list
		final List<CellTypeClassification> cellTypesFirstRound = cellTypesPerRound.get(0);
		cellTypesFirstRound.stream()
				.filter(cellType -> cellType.getEnrichmentFDR() <= CELL_TYPE_FDR_SIGNIFICANCE_THRESHOLD)
				.forEach(cellType -> cellTypeNames.add(cellType.getName()));
		// now we keep removing the ones that are not in the set for the subsequent
		// rounds
		for (int i = 1; i < cellTypesPerRound.size(); i++) {
			final List<CellTypeClassification> cellTypesNextRound = cellTypesPerRound.get(i);
			for (final CellTypeClassification cellType : cellTypesNextRound) {
				if (cellType.getEnrichmentFDR() <= CELL_TYPE_FDR_SIGNIFICANCE_THRESHOLD) {
					if (!cellTypeNames.contains(cellType.getName())) {
						cellTypeNames.remove(cellType.getName());
					}
				}
			}
		}

		// now we keep the ones rom the first round that have the name
		for (final CellTypeClassification cellType : cellTypesFirstRound) {
			if (cellTypeNames.contains(cellType.getName())) {
				validCellTypes.add(cellType);
			}
		}
		return validCellTypes;
	}

	/**
	 * Whether the {@link CellTypeClassification} has an FDR <= than the threshold
	 * 
	 * @param cellType
	 * @param fdrThreshold
	 * @return
	 */
	private boolean passFDRThreshold(CellTypeClassification cellType, double fdrThreshold) {
		return cellType.getEnrichmentFDR() <= fdrThreshold;
	}

	/**
	 * It performs a post analysis on the cell type classifications:<br>
	 * It looks at the input genes and how significant they are represented in each
	 * cell type by calculating a hyper geometric test
	 * 
	 * @param cellTypeClassifications
	 * @param totalNumSingleCells
	 */
	private void postAnalysis(List<CellTypeClassification> cellTypeClassifications, int totalNumSingleCells) {
		final CellTypeAndGenesDBUtil cellTypeandGenesDBUtil = CellTypeAndGenesDBUtil.getInstance(cellTypeGeneRepo,
				expressionMongoRepo);
		final TObjectIntMap<String> cellsPerGene = new TObjectIntHashMap<String>();
		for (final CellTypeClassification cellType : cellTypeClassifications) {

			if (passFDRThreshold(cellType, CELL_TYPE_FDR_SIGNIFICANCE_THRESHOLD)) {
				int numGenesSignificantInType = 0;

				final long num_cells_type_total = singleCellMongoRepo.countByType(cellType.getName());
				final TShortList geneIDs = interactorExpressions.getInteractorsGeneIDs();
				for (final short geneID : geneIDs.toArray()) {
					final String geneName = interactorExpressions.getGeneName(geneID);
					final long num_cells_gene_type = cellTypeandGenesDBUtil.countCellsByGeneAndCellType("HCL",
							cellType.getName(), geneName);

					int num_cells_gene_total = -1;
					if (cellsPerGene.containsKey(geneName)) {
						num_cells_gene_total = cellsPerGene.get(geneName);
					} else {
						num_cells_gene_total = (int) expressionMongoRepo.countByGene(geneName);
						cellsPerGene.put(geneName, num_cells_gene_total);
					}

					// K = successes in the population
					// n = sample size
					// k = successes in the sample
					//
					// K = number of single cells that have GRIA
					// n = number of single cells of type X
					// k = number of single cells that have GRIA and that are of type X

					final int N = totalNumSingleCells;
					final int K = num_cells_gene_total;
					final int n = (int) num_cells_type_total;
					final int k = (int) num_cells_gene_type;
					final HypergeometricDistribution hg = new HypergeometricDistribution(N, K, n);
					final double p = hg.upperCumulativeProbability(k);
					if (p < 0.001) {
						numGenesSignificantInType++;
					}
				}
				cellType.setNumGenesSignificant(numGenesSignificantInType);
			}
		}
	}

	private void calculateQuickScore(List<CellTypeClassification> cellTypeClassifications,
			ScoreThreshold scoreThreshold) {
		for (final CellTypeClassification cellTypeClassification : cellTypeClassifications) {
			final List<GeneOccurrence> genes = cellTypeClassification
					.getRankingOfGenesThatContributedToTheScore(scoreThreshold);
			float quickScore = 0;
			for (final GeneOccurrence geneOccurrence : genes) {
				final String geneName = geneOccurrence.getGene();
				final short geneID = InteractorsExpressionsRetriever.getInstance().getGeneID(geneName);
				final int num_cells = InteractorsExpressionsRetriever.getInstance().getExpressionsOfGene(geneID)
						.getSingleCellsIDs().size();
				final int num_cells_of_type = geneOccurrence.getOccurrence();
				final float p = 1.0f * num_cells / num_cells_of_type;
				if (quickScore == 0) {
					quickScore = p;
				} else {
					quickScore *= p;
				}
			}
			cellTypeClassification.setEnrichmentScore(quickScore);
		}

	}

	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	private void logInputParams(InputParameters inputParameters) {
		final String msg = "Input parameters: " + inputParameters;
		logStatus(msg, LogLevel.INFO);
	}

	private Date getDateNow() {
		return Calendar.getInstance(TimeZone.getDefault()).getTime();
	}

	private InputParameters getInputParameters() {
		final InputParameters inputParameters = new InputParameters();
		inputParameters.setCellTypeBranch(cellTypeBranch);
		inputParameters.setEmail(email);
		inputParameters.setInputDataFile(FilenameUtils.getName(experimentExpressionFile.getAbsolutePath()));
		inputParameters.setLoadRandom(loadRandomDistributionsIfExist);
		for (final ScoringSchema scoringSchema : sequentialScoringSchemas) {
			inputParameters.addScoringSchema(scoringSchema);
		}
		inputParameters.setDataset(dataset);
		inputParameters.setNumPermutations(maxIterations);
		inputParameters.setOutputPrefix(prefix);
		inputParameters.setPlotNegativeEnriched(plotNegativeEnrichedCellTypes);
		return inputParameters;
	}

	public static final FastDateFormat dateFormatter = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss");
	public static final String NEGATIVE_EWS_FROM_HERE = "------------------------- Negative ews from here -------------------------";
	public static final int UMAP_DIMENSIONS = 4;

	private String createTimeStamp(String prefix) {
		return FileUtils.checkInvalidCharacterNameForFileName(dateFormatter.format(new Date()) + "_" + prefix);
	}

	/**
	 * Delete all files and folders inside a folder.
	 * 
	 * @param folder
	 * @param notToDelete
	 * @param deleteFolder if true, the folder as input parameter will be also
	 *                     deleted after all its content is deleted.
	 * @return
	 */
	private int deleteFilesOnFolderContectRecursively(File folder, File notToDelete, boolean deleteFolder)
			throws IOException {
		int ret = 0;
		if (folder.isDirectory()) {
			final File[] listFiles = folder.listFiles();
			for (final File file : listFiles) {
				if (file.isFile()) {
					if (file.compareTo(notToDelete) != 0) {
						file.delete();
						ret++;
					}
				} else {
					ret += deleteFilesOnFolderContectRecursively(file, notToDelete, true);
				}
			}
			if (deleteFolder) {
				folder.delete();
			}
		}
		return ret;
	}

	private void writeGZipOutputFile(File zipFile, ScoringSchema scoringSchema) throws IOException {
		final File folder = getCurrentTimeStampFolder(scoringSchema.getScoringMethod());
		logStatus("Compacting output files in single zip file...");
		// move out the correlations file so that is not included in the zip file

		final Set<String> fileNames = new THashSet<String>();

		final String fileName = FilenameUtils
				.getName(getScoresOutputFile(scoringSchema.getScoringMethod()).getAbsolutePath());
		fileNames.add(fileName);

		// it may be or not
		final File[] scoreFiles = folder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {

				if (fileNames.contains(name)) {
					return true;
				}
				return false;
			}
		});
		final List<File> tempFiles = new ArrayList<File>();
		if (scoreFiles.length > 0) {

			for (int i = 0; i < scoreFiles.length; i++) {
				final File file = scoreFiles[i];
				final File tempFile = File.createTempFile("correlations_" + i, "txt");
				tempFiles.add(tempFile);
				Files.move(file, tempFile);
			}

		}
		ZipManager.addFileToZipFile(folder, zipFile, true);
		if (scoreFiles.length > 0) {
			for (int i = 0; i < scoreFiles.length; i++) {

				// move back
				Files.move(tempFiles.get(i), scoreFiles[i]);
			}

		}
		logStatus("Compressed file created at: " + zipFile.getAbsolutePath() + " ("
				+ FileUtils.getDescriptiveSizeFromBytes(zipFile.length()) + ")");
	}

	private void printGenesInvolvedInScores(List<CellTypeClassification> cellTypeClassifications,
			ScoringSchema scoringSchema) throws IOException {
		final File outputFile = getGenesInvolvedInCorrelationsOutputFile(scoringSchema.getScoringMethod());
		FileWriter fw = null;
		BufferedWriter buffer = null;
		try {
			ConcurrentUtil.sleep(1L);
			fw = new FileWriter(outputFile);
			buffer = new BufferedWriter(fw);

			// header
			buffer.write("cell_type\tgene\tocurrence\tpct_occurrence\n");

			// table
			for (final CellTypeClassification cellType : cellTypeClassifications) {
				final List<GeneOccurrence> geneOccurrences = cellType
						.getRankingOfGenesThatContributedToTheScore(scoringSchema.getScoringThreshold());
				for (final GeneOccurrence geneOccurrence : geneOccurrences) {

					final String gene = geneOccurrence.getGene();
					final short geneID = InteractorsExpressionsRetriever.getInstance().getGeneID(gene);
					final Gene geneObject = InteractorsExpressionsRetriever.getInstance().getExpressionsOfGene(geneID);
					final int numSingleCellsInWhichIsExpressed = geneObject
							.getNumSingleCellsInWhichIsExpressed(cellType.getName());
					final long numSingleCellsOfType = cellType.getNumCellsOfTypePassingCorrelationThreshold();
					final double pct = numSingleCellsInWhichIsExpressed * 1.0 / numSingleCellsOfType;
					buffer.write(cellType.getName() + "\t" + gene + "\t" + geneOccurrence.getOccurrence() + "\t" + pct
							+ "\n");
				}
			}
			log.info("File with genes contributing to the score in each cell type wrote at: "
					+ outputFile.getAbsolutePath());
		} finally {
			if (buffer != null) {
				buffer.close();
			}
		}
	}

	private void umapClustering(List<CellTypeClassification> cellTypeClassifications, ScoringSchema scoringSchema) {
		final List<CellTypeClassification> cellTypesWithPositiveEnrichmentWeigthedScore = cellTypeClassifications
				.stream().filter(ct -> ct.getEnrichmentScore() > 0.0f).collect(Collectors.toList());
		logStatus(cellTypesWithPositiveEnrichmentWeigthedScore.size()
				+ " cell types with positive enrichment weigthed score");
		logStatus("Performing UMAP clusterings with different thresholds...");
		umapClustering(cellTypesWithPositiveEnrichmentWeigthedScore, scoringSchema,
				"UMAP clustering of all cell types (no sig threshold)", true, "all");

		// by significancy < 0.01
		List<CellTypeClassification> significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> ct.getEnrichmentSignificance() < 0.01).collect(Collectors.toList());
		umapClustering(significantCellTypes, scoringSchema, "UMAP clustering of significant cell types (sig<0.01)",
				false, "sig_0.01");
		// by significancy < 0.05
		significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> ct.getEnrichmentSignificance() < 0.05).collect(Collectors.toList());
		umapClustering(significantCellTypes, scoringSchema, "UMAP clustering of significant cell types (sig<0.05)",
				false, "sig_0.05");
		// by hypergeometric p-value < 0.05
		significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> ct.getHypergeometricPValue() < 0.05).collect(Collectors.toList());
		umapClustering(significantCellTypes, scoringSchema,
				"UMAP clustering of significant cell types by hypergeometric test (p-value<0.05)", false,
				"hypG_pvalue_0.05");
		// by Kolmogorov-Smirnov test: take only the ones with *,** or ***
		significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> !"".equals(ct.getSignificancyString())).collect(Collectors.toList());
		umapClustering(significantCellTypes, scoringSchema,
				"UMAP clustering of significant cell types by KS test (*, ** or ***)", false, "sig_KStest");

	}

	/**
	 * Creates an histogram plot with the values of the suprema for all cell types
	 * that has a positive enrichment score.
	 * 
	 * @param cellTypeClassifications
	 * @param plotNegativeEnrichedCellTypes
	 */
	private void createHistogramOfSuprema(List<CellTypeClassification> cellTypeClassifications,
			boolean plotNegativeEnrichedCellTypes, ScoringMethod scoringMethod) {
		final List<CellTypeClassification> cellTypes = cellTypeClassifications.stream()
				.filter(ct -> plotNegativeEnrichedCellTypes || ct.getEnrichmentScore() > 0.0f)
				.collect(Collectors.toList());
		try {
			final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(getResultsSubfolderGeneral(scoringMethod),
					"suprema_hist", prefix);
			final FileWriter fw = new FileWriter(outputTXTFile);
			fw.write("cell type\tsupremumX\n");
			for (final CellTypeClassification cellType : cellTypes) {
				final int supremumX = cellType.getSupremumX();
				if (supremumX != -1) {
					fw.write(cellType.getName() + "\t" + supremumX + "\n");
				}
			}

			fw.close();

			logStatus("Histogram of suprema's positions in x axis created.");

		} catch (final IOException e) {
			e.printStackTrace();
			logStatus("Some error occurred while creating chart for histogram of correlating genes: " + e.getMessage(),
					LogLevel.ERROR);
		}
	}

	/**
	 * Creates an scatter plot in which x axis is the position of the suprema and
	 * y-axis is the size of the suprema. One series per cell type.
	 * 
	 * @param cellTypeClassifications
	 * @param plotNegativeEnrichedCellTypes
	 */
	private void createScatterPlotOfSuprema(List<CellTypeClassification> cellTypeClassifications,
			boolean plotNegativeEnrichedCellTypes, ScoringMethod scoringMethod) {
		final List<CellTypeClassification> cellTypes = cellTypeClassifications.stream()
				.filter(ct -> plotNegativeEnrichedCellTypes || ct.getEnrichmentScore() > 0.0f)
				.collect(Collectors.toList());
		try {
			final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(getResultsSubfolderGeneral(scoringMethod),
					"suprema_scatter", prefix);
			final FileWriter fw = new FileWriter(outputTXTFile);
			fw.write("cell type\tSuprema position in ranked cell list\tsupremum size\n");
			final TDoubleList supremaXs = new TDoubleArrayList();
			for (final CellTypeClassification cellType : cellTypes) {
				final int supremumX = cellType.getSupremumX();
				final double supremum = cellType.getEnrichmentScore();
				supremaXs.add(supremumX);

				final String label = cellType.getName();
				fw.write(label + "\t" + supremumX + "\t" + supremum + "\n");

			}
			fw.close();
			logStatus("Scatter plot of suprema created.");
		} catch (final IOException e) {
			e.printStackTrace();
			logStatus("Some error occurred while creating chart for UMAP clustering: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	/**
	 * This creates a chart with an histogram of number of genes used for
	 * correlation for all single cells. In this case, it is a
	 * ScoringMethod.PEARSONS_CORRELATION
	 * 
	 * @param singleCellList
	 */
	private void createDistributionChartOfCorrelationsOverRankedCells(List<SingleCell> singleCellList,
			ScoringSchema scoringSchema) {
		try {
			if (scoringSchema.getScoringMethod() != ScoringMethod.PEARSONS_CORRELATION) {
				throw new IllegalArgumentException("This function is only for pearsons correlation method");
			}
			final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(
					getResultsSubfolderGeneral(scoringSchema.getScoringMethod()), "corr_rank_dist", prefix);
			final FileWriter fw = new FileWriter(outputTXTFile);
			fw.write("rank\tclass\t" + ScoringMethod.PEARSONS_CORRELATION.getScoreName() + "\n");
			PCTSEAUtils.sortByScoreDescending(singleCellList);

			final ScoreThreshold scoringThreshold = scoringSchema.getScoringThreshold();
			final String positiveKey = "corr >= " + scoringThreshold.getThresholdValue();
			final String negativeKey = "corr < " + scoringThreshold.getThresholdValue();

			int numCell = 1;
			ConcurrentUtil.sleep(1L);
			for (final SingleCell singleCell : singleCellList) {
				if (!Double.isNaN(singleCell.getScoreForRanking())) {
					if (scoringThreshold.passThreshold(singleCell)) {
						fw.write(numCell + "\t" + positiveKey + "\t" + singleCell.getScoreForRanking() + "\n");
					} else {
						fw.write(numCell + "\t" + negativeKey + "\t" + singleCell.getScoreForRanking() + "\n");
					}
					numCell++;
				}
			}
			fw.close();
			PCTSEA.logStatus("Rank of cells by Pearson's correlation plot of suprema created.");
		} catch (final IOException e) {
			e.printStackTrace();
			PCTSEA.logStatus("Some error occurred while creating chart for UMAP clustering: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	/**
	 * This creates a chart with an histogram of number of genes used for scores for
	 * all single cells
	 * 
	 * @param singleCellList
	 * @param scoringMethod
	 */
	private void createDistributionChartOfScoresOverRankedCells(List<SingleCell> singleCellList,
			ScoringSchema scoringSchema) {
		final ScoringMethod scoringMethod = scoringSchema.getScoringMethod();
		final ScoreThreshold scoringThreshold = scoringSchema.getScoringThreshold();
		try {

			final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(getResultsSubfolderGeneral(scoringMethod),
					"corr_rank_dist", prefix);
			final FileWriter fw = new FileWriter(outputTXTFile);
			fw.write("rank\tclass\t" + scoringMethod.getScoreName() + "\n");
			PCTSEAUtils.sortByScoreDescending(singleCellList);
			int numCell = 1;
			ConcurrentUtil.sleep(1L);
			for (final SingleCell singleCell : singleCellList) {
				if (!Double.isNaN(singleCell.getScoreForRanking())) {
					if (scoringThreshold.passThreshold(singleCell)) {
						fw.write(numCell + "\t" + scoringMethod.getScoreName() + "\t" + singleCell.getScoreForRanking()
								+ "\n");
					}
					numCell++;
				}
			}
			fw.close();
			PCTSEA.logStatus("Rank of cells by " + scoringMethod.getScoreName() + " plot created.");
		} catch (final IOException e) {
			e.printStackTrace();
			PCTSEA.logStatus("Some error occurred while creating chart for " + scoringMethod.getScoreName() + ": "
					+ e.getMessage(), LogLevel.ERROR);
		}
	}

	/**
	 * This creates a chart with an histogram of number of genes used for
	 * correlation for all single cells
	 * 
	 * @param singleCellList
	 * @param minCorrelation
	 * @param scoringMethod
	 */
	private void createHistogramOfCorrelatingGenes(List<SingleCell> singleCellList, Double minCorrelation,
			ScoringMethod scoringMethod) {

//		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		final TIntIntMap histogramData = new TIntIntHashMap();
		int totalCells = 0;
		ConcurrentUtil.sleep(1L);
		for (final SingleCell singleCell : singleCellList) {

			if (minCorrelation != null) {
				if (minCorrelation > singleCell.getScoreForRanking()) {
					continue;
				}
			}
			totalCells++;
			final int num = singleCell.getGenesUsedForScore().size();
			if (!histogramData.containsKey(num)) {
				histogramData.put(num, 1);
			} else {
				histogramData.put(num, 1 + histogramData.get(num));
			}
		}
		final TIntList keys = new TIntArrayList(histogramData.keys());
		keys.sort();
//		for (final int numGenes : keys.toArray()) {
//			final int frequency = histogramData.get(numGenes);
//			dataset.addValue(frequency, "# genes", String.valueOf(numGenes));
//		}
//		for (int i = 0; i < keys.size(); i++) {
//			int accumulativeNumGenes = 0;
//			for (int j = i; j < keys.size(); j++) {
//				accumulativeNumGenes += histogramData.get(keys.get(j));
//
//			}
//			dataset.addValue(accumulativeNumGenes, "# genes or more", String.valueOf(keys.get(i)));
//		}

		String title = "Distribution of # of genes correlating";
		if (minCorrelation != null) {
			title += " with only cells with corr >=" + minCorrelation;
		}
		title += " (" + totalCells + ")";
//		final JFreeChart chart = ChartFactory.createBarChart(title, "# genes " + score_name + " > threshold", "# cells",
//				dataset, PlotOrientation.VERTICAL, true, false, false);
//		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
//		plot.setBackgroundPaint(Color.white);
//		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
//		renderer.setDefaultItemLabelGenerator(new IntegerCategoryItemLabelGenerator());
//		renderer.setDefaultItemLabelsVisible(true);
//		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
//		renderer.setItemMargin(0.1);
//		renderer.setBarPainter(new StandardBarPainter());
		try {

			writeNumGenesHistogramFile(histogramData, scoringMethod);
//			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
//			if (generatePDFCharts) {
//				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
//				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
//			}
			// logStatus("Chart with the distribution of # of genes correlating is
			// created.");

		} catch (final IOException e) {
			e.printStackTrace();
			PCTSEA.logStatus(
					"Some error occurred while creating chart for histogram of correlating genes: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	private void writeNumGenesHistogramFile(TIntIntMap histogramOfNumGenes, ScoringMethod scoringMethod)
			throws IOException {
		final TIntIntMap histogramOfNumGenesAccumulative = new TIntIntHashMap();
		final TIntList keys = new TIntArrayList(histogramOfNumGenes.keys());
		keys.sort();
		for (int i = 0; i < keys.size(); i++) {
			int accumulativeNumGenes = 0;
			for (int j = i; j < keys.size(); j++) {
				accumulativeNumGenes += histogramOfNumGenes.get(keys.get(j));
			}
			histogramOfNumGenesAccumulative.put(keys.get(i), accumulativeNumGenes);
		}
		final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(getResultsSubfolderGeneral(scoringMethod), "genes_hist",
				prefix);
		final FileWriter fw = new FileWriter(outputTXTFile);
		final BufferedWriter buffer = new BufferedWriter(fw);

		buffer.write("-\t# genes with " + scoringMethod.getScoreName() + " > threshold\t# cells\n");
		for (final int numGenes : histogramOfNumGenes.keys()) {
			final int frequency = histogramOfNumGenes.get(numGenes);
			buffer.write("# genes\t" + numGenes + "\t" + frequency + "\n");
		}
		for (final int numGenes : histogramOfNumGenesAccumulative.keys()) {
			final int frequency = histogramOfNumGenesAccumulative.get(numGenes);
			buffer.write("# genes or more\t" + numGenes + "\t" + frequency + "\n");
		}
		buffer.close();
	}

	/**
	 * 
	 * @param cellTypeClassifications
	 * @param scoringSchema
	 * @param chartTitle
	 * @param setAsDefaultUMAPOnCellType if true, the umap coordinates will be the
	 *                                   ones reported in the table
	 * @param labelChartFile
	 */
	private void umapClustering(List<CellTypeClassification> cellTypeClassifications, ScoringSchema scoringSchema,
			String chartTitle, boolean setAsDefaultUMAPOnCellType, String labelChartFile) {
		if (cellTypeClassifications.isEmpty()) {
			return;
		}
		ConcurrentUtil.sleep(1L);
		final ScoreThreshold scoreThreshold = scoringSchema.getScoringThreshold();
		final Set<String> totalGeneSet = new THashSet<String>();
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			final List<GeneOccurrence> geneOccurrences = cellType
					.getRankingOfGenesThatContributedToTheScore(scoreThreshold);
			geneOccurrences.forEach(go -> totalGeneSet.add(go.getGene()));
		}
		final List<String> totalGeneList = totalGeneSet.stream().collect(Collectors.toList());

		final float[][] instances = new float[cellTypeClassifications.size()][totalGeneSet.size()];
		for (int i = 0; i < cellTypeClassifications.size(); i++) {
			final CellTypeClassification cellType = cellTypeClassifications.get(i);
			final List<GeneOccurrence> geneOccurrences = cellType
					.getRankingOfGenesThatContributedToTheScore(scoreThreshold);
			final Map<String, GeneOccurrence> occurrencesPerGene = new THashMap<String, GeneOccurrence>();
			geneOccurrences.forEach(go -> occurrencesPerGene.put(go.getGene(), go));
			final float[] instance = new float[totalGeneSet.size()];
			instances[i] = instance;
			for (int j = 0; j < totalGeneList.size(); j++) {

				final String gene = totalGeneList.get(j);
				if (occurrencesPerGene.containsKey(gene)) {
					final float value = 1.0f * occurrencesPerGene.get(gene).getOccurrence();
					instance[j] = value;
				} else {
					final float value = 0.0f;
					instance[j] = value;
				}
			}
		}
		// perform the umap
		final Umap umap = new Umap();
		// number of dimensions in result
		umap.setNumberComponents(UMAP_DIMENSIONS);
		// This determines the number of neighboring points used in local approximations
		// of manifold structure. Larger values will result in more global structure
		// being preserved at the loss of detailed local structure. In general this
		// parameter should often be in the range 5 to 50, with a choice of 10 to 15
		// being a sensible default.
		// Salva: lets put a 5% of the number of instances
		int numberNearestNeighbours = Double.valueOf(cellTypeClassifications.size() * 0.05).intValue();
		numberNearestNeighbours = Math.min(Math.max(3, numberNearestNeighbours), 50);
		umap.setNumberNearestNeighbours(numberNearestNeighbours);
		// This controls how tightly the embedding is allowed compress points together.
		// Larger values ensure embedded points are more evenly distributed, while
		// smaller values allow the algorithm to optimize more accurately with regard to
		// local structure. Sensible values are in the range 0.001 to 0.5, with 0.1
		// being a reasonable default.
		umap.setMinDist(0.1f);
		// This determines the choice of metric used to measure distance in the input
		// space. Default to a Euclidean metric
		umap.setMetric(EuclideanMetric.SINGLETON);
//		umap.setMetric(CorrelationMetric.SINGLETON);
		// use > 1 to enable parallelism
		umap.setThreads(threadCount);
		//
		final float[][] fitTransform = umap.fitTransform(instances);
		if (setAsDefaultUMAPOnCellType) {
			for (int i = 0; i < cellTypeClassifications.size(); i++) {

				final CellTypeClassification cellType = cellTypeClassifications.get(i);
				final float[] instance = fitTransform[i];
				cellType.setUmapClusteringComponents(instance);
			}
		}

//		logStatus("UMAP clustering of cell types based on genes done.");

		// now save an scatter plot
		final LabeledXYDataset dataset = new LabeledXYDataset();
		for (int i = 0; i < cellTypeClassifications.size(); i++) {

			final CellTypeClassification cellType = cellTypeClassifications.get(i);
			final float[] umapClustering = fitTransform[i];
			final String label = cellType.getName();

			dataset.add(cellType.getName(), umapClustering[0], umapClustering[1], label);
		}
		final boolean legend = false;
		final boolean tooltips = true;
		final boolean urls = false;
		final JFreeChart chart = ChartFactory.createScatterPlot(chartTitle, "UMAP x", "UMAP y", dataset,
				PlotOrientation.VERTICAL, legend, tooltips, urls);
		final XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		final XYItemRenderer renderer = plot.getRenderer();
		renderer.setDefaultItemLabelGenerator(new LabelGenerator());
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
		chart.addSubtitle(new TextTitle(
				"(" + cellTypeClassifications.size() + " cell types | param nnn=" + numberNearestNeighbours + ")"));
		try {
			final String fileName = "umap_" + labelChartFile + "_scatter";
			writeGlobalUMAPDimensionsChart(getResultsSubfolderGeneral(scoringSchema.getScoringMethod()), prefix,
					fileName, cellTypeClassifications, fitTransform);
//			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
//			if (generatePDFCharts) {
//				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
//				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
//			}
			// logStatus("UMAP clustering chart created");

		} catch (final IOException e) {
			e.printStackTrace();
			logStatus("Some error occurred while creating chart for UMAP clustering: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	private void writeGlobalUMAPDimensionsChart(File resultsSubfolder, String prefix2, String fileName,
			List<CellTypeClassification> cellTypeClassifications, float[][] fitTransform) throws IOException {
		final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(resultsSubfolder, fileName, prefix2);
		final FileWriter fw = new FileWriter(outputTXTFile);
		final BufferedWriter buffer = new BufferedWriter(fw);

		buffer.write("cell_type");
		for (int umapDimension = 1; umapDimension <= fitTransform[0].length; umapDimension++) {
			buffer.write("\tUmap_" + umapDimension);
		}
		buffer.write("\n");
		for (int i = 0; i < cellTypeClassifications.size(); i++) {
			final String cellType = cellTypeClassifications.get(i).getName();
			final float[] umapClustering = fitTransform[i];
			buffer.write(cellType);
			for (final float umapDimension : umapClustering) {
				fw.write("\t" + umapDimension);
			}
			buffer.write("\n");
		}

		buffer.close();
	}

	/**
	 * Gets the list of single cells from the database of an specific
	 * {@link Dataset}
	 * 
	 * @param dataset
	 * @param cellTypeBranch
	 * @param numInputGenes  this is to reserve space in the single cell object for
	 *                       a maximum of that number of expressions
	 * @return
	 */
	private List<SingleCell> getSingleCellListFromDB(Dataset dataset, CellTypeBranch cellTypeBranch,
			int numInputGenes) {

		final long t0 = System.currentTimeMillis();

		logStatus("Getting single cells from DB...");

		final List<SingleCell> ret = new ArrayList<SingleCell>();
		final List<edu.scripps.yates.pctsea.db.SingleCell> singleCellsFromDB = new ArrayList<edu.scripps.yates.pctsea.db.SingleCell>();
		if (dataset != null) {

			singleCellsFromDB.addAll(singleCellMongoRepo.findByDatasetTag(dataset.getTag()));

		} else {
			singleCellsFromDB.addAll(singleCellMongoRepo.findAll());
		}
		logStatus(singleCellsFromDB.size() + " cells retrieved");
		logStatus("Processing information from cells...");
		int cellID = 0;
		final ProgressCounter counter = new ProgressCounter(singleCellsFromDB.size(),
				ProgressPrintingType.PERCENTAGE_STEPS, 0, true);
		for (final edu.scripps.yates.pctsea.db.SingleCell singleCelldb : singleCellsFromDB) {
			counter.increment();
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				ConcurrentUtil.sleep(2L);
			}

//			if (singleCelldb.getType() == null) {
//				// ignore single cell because it is not categorized
//				try {
//					final FileWriter fw = new FileWriter(new File(
//							"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctsea-core\\src\\main\\resources\\cells_with_no_type.txt"),
//							true);
//					fw.write(singleCelldb.getId() + "\t" + singleCelldb.getName() + "\n");
//					fw.close();
//				} catch (final IOException e) {
//					e.printStackTrace();
//				}
//				continue;
//			}
			cellID++;

			final SingleCell sc = new SingleCell(cellID, singleCelldb.getName(), Double.NaN, numInputGenes);

			sc.setCellType(singleCelldb.getType(), true, cellTypeBranch);
			ret.add(sc);
			SingleCellsMetaInformationReader.addSingleCell(sc);
		}
		final long t1 = System.currentTimeMillis();
		logStatus(ret.size() + " single cells read from database in "
				+ DatesUtil.getDescriptiveTimeFromMillisecs(t1 - t0));

		return ret;

	}

	/**
	 * 
	 * @param interactorExpressions
	 * @param cellTypeClassifications
	 * @param singleCellListPassingCorrelationThreshold
	 * @param cellTypeBranch
	 * @param maxIterations
	 * @param loadRandomDistributionsIfExist
	 * @param plotNegativeEnrichedCellTypes
	 * @throws IOException
	 */
	private void calculateSignificanceByCellTypesPermutations(InteractorsExpressionsRetriever interactorExpressions,
			List<CellTypeClassification> cellTypeClassifications, List<SingleCell> singleCellsPassingScoreThreshold,
			CellTypeBranch cellTypeBranch, int maxIterations, boolean loadRandomDistributionsIfExist,
			boolean plotNegativeEnrichedCellTypes, ScoringMethod scoringMethod) throws IOException {

		final File randomScoresFile = getRandomScoresFile(scoringMethod);
		if (loadRandomDistributionsIfExist && randomScoresFile.exists() && randomScoresFile.length() > 0l) {
			readRandomDistributionFile(randomScoresFile, cellTypeClassifications);
		} else {

			final List<String> originalCellTypes = new ArrayList<String>();
			singleCellsPassingScoreThreshold.forEach(c -> originalCellTypes.add(c.getCellType()));

			final boolean outputToLog = false;
			int iteration = 1;
			final ProgressCounter counter = new ProgressCounter(maxIterations, ProgressPrintingType.PERCENTAGE_STEPS, 0,
					true);
			counter.setSuffix("performing cell type permutations to calculate significance of enrichment...");
			while (iteration <= maxIterations) {
				try {

					// permute cell types of the cell types that pass the threshold only

					final List<String> permutatedCellTypes = new ArrayList<String>();
					permutatedCellTypes.addAll(originalCellTypes);
					Collections.shuffle(permutatedCellTypes);
					for (int i = 0; i < singleCellsPassingScoreThreshold.size(); i++) {
						final String permutatedCellType = permutatedCellTypes.get(i);
						// we dont want to use branchedCellTypes or parsetypos
						final boolean parseTypos = false;
						final CellTypeBranch cellTypeBranch2 = null;
						singleCellsPassingScoreThreshold.get(i).setCellType(permutatedCellType, parseTypos,
								cellTypeBranch2);
					}

					// calculate enrichment scores with the Kolmogorov-Smirnov test

					calculateEnrichmentScore(cellTypeClassifications, singleCellsPassingScoreThreshold, false,
							outputToLog, true, plotNegativeEnrichedCellTypes, scoringMethod);

					counter.increment();
					final String printIfNecessary = counter.printIfNecessary();
					if (!"".equals(printIfNecessary)) {
						PCTSEA.logStatus(printIfNecessary, false);
					}
				} finally {
					iteration++;
				}

			}

			printToRandomDistributionFile(randomScoresFile, cellTypeClassifications);
			PCTSEA.logStatus("Iterations  finished. Random scores distributions are stored at: "
					+ randomScoresFile.getAbsolutePath());
			// we set back the original correlations values
			for (int i = 0; i < singleCellsPassingScoreThreshold.size(); i++) {
				final String originalCellType = originalCellTypes.get(i);
				// we dont want to parse for typos or use branched cell type because it is a
				// permutation
				final boolean parseForTypos = false;
				final CellTypeBranch cellTypeBranch2 = null;
				singleCellsPassingScoreThreshold.get(i).setCellType(originalCellType, parseForTypos, cellTypeBranch2);
			}
		}

//		logStatus("Normalizing enrichment scores by cell type sizes...");
		// now we have all the random distributions. we can normalize by size.
		// we do that by dividing the real scores by the average of the random scores
		// this is done now when calling to cellType.getNormalizedEnrichmentScore()
		final boolean onlyTakePositiveScores = true;
		PCTSEA.logStatus("Calculating enrichment scores significance...");
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			final float realScore = cellType.getEnrichmentScore();
			if (Float.isNaN(realScore)) {
				cellType.setEnrichmentSignificance(Double.NaN);
				continue;
			}

			final TFloatList randomEnrichmentScores = cellType.getRandomEnrichmentScores();
			final TFloatList randomEnrichmentScores2 = new TFloatArrayList();
			for (final float score : randomEnrichmentScores.toArray()) {
				if (!Float.isNaN(score)) {
					randomEnrichmentScores2.add(score);
				}
			}
			double pvalue = Double.NaN;
			if (onlyTakePositiveScores) {

				if (realScore >= 0.0f) {
					pvalue = calculateProportionPositive(realScore, randomEnrichmentScores2);
				} else {
					pvalue = calculateProportionNegative(realScore, randomEnrichmentScores2);
				}
			} else {
				pvalue = calculateProportionAll(realScore, randomEnrichmentScores2);
			}
			cellType.setEnrichmentSignificance(pvalue);
		}

		// FDR calculation. For this, we use all the random statistics and all the real
		// statistics
		PCTSEA.logStatus("Calculating False Discovery Rate for multiple hypothesis correction...");
		final TFloatList totalRealNormalizedScores = new TFloatArrayList();
		final TFloatList totalRandomNormalizedScores = new TFloatArrayList();
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			if (Float.isNaN(cellType.getNormalizedEnrichmentScore())) {
				continue;
			}

			totalRealNormalizedScores.add(cellType.getNormalizedEnrichmentScore());
			totalRandomNormalizedScores.addAll(cellType.getNormalizedRandomEnrichmentScores());

		}
		if (onlyTakePositiveScores) {
			// remove the negative ones
			TFloatIterator iterator = totalRealNormalizedScores.iterator();
			while (iterator.hasNext()) {
				final float score = iterator.next();
				if (score < 0f) {
					iterator.remove();
				}
			}
			// remove the negative ones
			iterator = totalRandomNormalizedScores.iterator();
			while (iterator.hasNext()) {
				final float score = iterator.next();
				if (score < 0f) {
					iterator.remove();
				}
			}
		}
		createFDRCalculationPlot(totalRealNormalizedScores, totalRandomNormalizedScores, scoringMethod);
		// now, for each real one, we calculate FDR as
		// FDR = (snull / sobs) * (nobs / nnull), where
		// snull is the number of random scores that are equal or greater than the real
		// score
		// sobs is the number of real scores that are equal or greater than the real
		// score
		// nobs is the total number of real scores
		// nnull is the total number of random scores
		final int nobs = totalRealNormalizedScores.size();
		final int nnull = totalRandomNormalizedScores.size();
		// we sort the arrays so that we can do binary searches
		final float[] totalRealNormalizedScoresArray = totalRealNormalizedScores.toArray();
		Arrays.sort(totalRealNormalizedScoresArray);
		final float[] totalRandomNormalizedScoresArray = totalRandomNormalizedScores.toArray();
		Arrays.sort(totalRandomNormalizedScoresArray);

		for (final CellTypeClassification cellType : cellTypeClassifications) {
			final float realNormalizedScore = cellType.getNormalizedEnrichmentScore();
			if (Float.isNaN(realNormalizedScore) || realNormalizedScore < 0f) {
				cellType.setEnrichmentFDR(Double.NaN);
				continue;
			}
			final int index = Arrays.binarySearch(totalRealNormalizedScoresArray, realNormalizedScore);
			final int index2 = Arrays.binarySearch(totalRandomNormalizedScoresArray, realNormalizedScore);

			final int sobs = nobs - (index >= 0 ? index + 1 : -index - 1);

			final int snull = nnull - (index2 >= 0 ? index2 + 1 : -index2 - 1);

			double fdr = Double.NaN;
			if (snull == 0) {
				fdr = 0.0;
			} else {
				fdr = (1.0 * snull / sobs) * (1.0 * nobs / nnull);
			}

			cellType.setEnrichmentFDR(fdr);
		}

		// now calculate the BH-corrected values for KS pvalues
		final TDoubleList pvalues = new TDoubleArrayList();
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			pvalues.add(cellType.getKSTestPvalue());
		}
		final double[] pAdjusted = PValueCorrection.pAdjust(pvalues.toArray(), PValueCorrectionType.BH);
		for (int i = 0; i < cellTypeClassifications.size(); i++) {
			final double ksTestCorrectedPValue = pAdjusted[i];
			final CellTypeClassification cellType = cellTypeClassifications.get(i);
			cellType.setKSTestCorrectedPvalue(ksTestCorrectedPValue);
			String ksSignificancyString = "";
			if (cellType.getKSTestCorrectedPvalue() < 0.001) {
				ksSignificancyString = "***";
			} else if (cellType.getKSTestCorrectedPvalue() < 0.01) {
				ksSignificancyString = "**";
			} else if (cellType.getKSTestCorrectedPvalue() < 0.05) {
				ksSignificancyString = "*";
			}
			cellType.setKSTestSignificancyString(ksSignificancyString);
		}

	}

	private JFreeChart createFDRCalculationPlot(TFloatList totalRealNormalizedScores,
			TFloatList totalRandomNormalizedScores, ScoringMethod scoringMethod) {
		PCTSEA.logStatus("Creating FDR calculation plot with " + totalRealNormalizedScores.size() + " real scores and "
				+ totalRandomNormalizedScores.size() + " random scores");
		// create chart
//		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

//		final DecimalFormat format = new DecimalFormat("#.#");
//
//		final int numBins = Math.max(Histogram.getSturgisRuleForHistogramBins(
//				Math.max(totalRealNormalizedScores.size(), totalRandomNormalizedScores.size())), 10);
//
//		final float max = Math.max(totalRealNormalizedScores.max(), totalRandomNormalizedScores.max());
//		final float min = Math.min(totalRealNormalizedScores.min(), totalRandomNormalizedScores.min());
//		double[][] binStats = Histogram.calcHistogram(totalRealNormalizedScores.toArray(), min, max, numBins);
//		for (int i = 0; i < binStats[0].length; i++) {
//			final double bin = binStats[2][i] / totalRealNormalizedScores.size();
//
//			final double lowerBound = binStats[0][i];
//			final double upperBound = binStats[1][i];
//			final String columnKey = format.format(lowerBound) + " / " + format.format(upperBound);
//			dataset.addValue(bin, "Observed", columnKey);
//		}
		// TODO
		// no others as requested by Casimir
//				histogramDataset.addSeries(seriesOhersType);
//		binStats = Histogram.calcHistogram(totalRandomNormalizedScores.toArray(), min, max, numBins);
//		for (int i = 0; i < binStats[0].length; i++) {
//			final double bin = binStats[2][i] / totalRandomNormalizedScores.size();
//			final double lowerBound = binStats[0][i];
//			final double upperBound = binStats[1][i];
//			final String columnKey = format.format(lowerBound) + " / " + format.format(upperBound);
//			dataset.addValue(bin, "Null", columnKey);
//		}
//		final String plotTitle = "Multiple testing correction";
//		final String xaxis = "Normalized Enrichment Scores";
//		final String yaxis = "Normalized Frequency";
//		final PlotOrientation orientation = PlotOrientation.VERTICAL;
//		final boolean show = true;
//		final boolean toolTips = false;
//		final boolean urls = false;

//		final JFreeChart chart = ChartFactory.createBarChart(plotTitle, xaxis, yaxis, dataset, orientation, show,
//				toolTips, urls);
//		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
//		plot.setBackgroundPaint(Color.white);
//		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
//		renderer.setDefaultItemLabelGenerator(new DoubleCategoryItemLabelGenerator("#.##"));
//		renderer.setDefaultItemLabelsVisible(true);
//		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
//		renderer.setItemMargin(0.1);
//		renderer.setBarPainter(new StandardBarPainter());
//		final CategoryAxis domainAxis = plot.getDomainAxis();
//		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

//		final ChartPanel panel = new ChartPanel(chart);
//		final JFrame frame = new JFrame();
//		frame.add(panel);
//		frame.setVisible(true);
		try {
			final String fileName = "ews_obs_null_hist";
//			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			writeGlobalFDRCalculationChart(getResultsSubfolderGeneral(scoringMethod), prefix, fileName,
					totalRealNormalizedScores, totalRandomNormalizedScores);

		} catch (final IOException e) {
			e.printStackTrace();
			PCTSEA.logStatus("Some error occurred while creating chart for the multiple hypothesis correction: "
					+ e.getMessage(), LogLevel.ERROR);
		}
		return null;
//		return chart;
	}

	private void writeGlobalFDRCalculationChart(File resultsSubfolder, String prefix2, String fileName,
			TFloatList totalRealNormalizedScores, TFloatList totalRandomNormalizedScores) throws IOException {
		final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(resultsSubfolder, fileName, prefix2);
		final FileWriter fw = new FileWriter(outputTXTFile);
		final BufferedWriter buffer = new BufferedWriter(fw);
		for (final float score : totalRealNormalizedScores.toArray()) {
			buffer.write("Observed\t" + score + "\n");
		}
		for (final float score : totalRandomNormalizedScores.toArray()) {
			buffer.write("Random\t" + score + "\n");
		}
		buffer.close();

	}

	/**
	 * 
	 * @param interactorExpressions
	 * @param cellTypeClassifications
	 * @param singleCellList
	 * @param cellTypeBranch
	 * @param scoreThreshold
	 * @param loadRandomDistributionsIfExist
	 * @param plotNegativeEnrichedCellTypes
	 * @param maxIterations
	 * @param takeZerosForCorrelation
	 * @param minNumberExpressedGenesInCell
	 * @throws IOException
	 */
	private void calculateSignificanceByPhenotypePermutations(InteractorsExpressionsRetriever interactorExpressions,
			List<CellTypeClassification> cellTypeClassifications, List<SingleCell> singleCellList,
			CellTypeBranch cellTypeBranch, ScoringSchema scoreSchema, boolean loadRandomDistributionsIfExist,
			boolean plotNegativeEnrichedCellTypes, int maxIterations, boolean takeZerosForCorrelation)
			throws IOException {

		// as well as the correlations
		final TIntDoubleMap correlationsBySingleCellID = new TIntDoubleHashMap();
		for (final SingleCell singleCell : singleCellList) {
			correlationsBySingleCellID.put(singleCell.getId(), singleCell.getScoreForRanking());
		}

		final File randomScoresFile = getRandomScoresFile(scoreSchema.getScoringMethod());
		if (loadRandomDistributionsIfExist && randomScoresFile.exists() && randomScoresFile.length() > 0l) {
			readRandomDistributionFile(randomScoresFile, cellTypeClassifications);
		} else {
			final boolean outputToLog = false;
			int iteration = 1;
			final ProgressCounter counter = new ProgressCounter(maxIterations, ProgressPrintingType.PERCENTAGE_STEPS, 0,
					true);
			counter.setSuffix("performing phenotype permutations to calculate significant enrichment scores...");
			while (iteration <= maxIterations) {
				try {

					// permute gene expressions in each cell
					interactorExpressions.permuteSingleCellExpressions();

					// calculate correlations
					calculateScoresToRankSingleCells(singleCellList, interactorExpressions, scoreSchema, false,
							outputToLog, false, takeZerosForCorrelation);
					List<SingleCell> singleCellsPassingScoreThreshold = scoreSchema.getScoringThreshold()
							.getSingleCellsPassingThresholdSortedByScore(singleCellList);
					singleCellsPassingScoreThreshold = singleCellsPassingScoreThreshold.subList(0,
							Math.min(singleCellsPassingScoreThreshold.size() - 1, MAX_SINGLE_CELLS_FOR_SCORE));
					// calculate enrichment scores with the Kolmogorov-Smirnov test
					calculateEnrichmentScore(cellTypeClassifications, singleCellsPassingScoreThreshold, false,
							outputToLog, true, plotNegativeEnrichedCellTypes, scoreSchema.getScoringMethod());

					counter.increment();
					final String printIfNecessary = counter.printIfNecessary();
					if (!"".equals(printIfNecessary)) {
						logStatus(printIfNecessary);
					}
				} finally {
					iteration++;
				}

			}
			logStatus("Iterations  finished. Random scores distributions are stored at: "
					+ randomScoresFile.getAbsolutePath());
			printToRandomDistributionFile(randomScoresFile, cellTypeClassifications);
		}
		// we set back the original correlations values
		for (final SingleCell singleCell : singleCellList) {
			singleCell.setScore(correlationsBySingleCellID.get(singleCell.getId()));
		}
		// now we have all the distributions
		logStatus("Calculating enrichment scores significancy...");
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			final float realScore = cellType.getEnrichmentScore();
			boolean negative = false;

			negative = true;

			final TFloatList scoreDistributions = cellType.getRandomEnrichmentScores();

			final TFloatList doubleScoreDistributions = new TFloatArrayList();
			for (final float score : scoreDistributions.toArray()) {
				if (!Float.isNaN(score)) {
					doubleScoreDistributions.add(score);
				}
			}
			final boolean onlyTakePositiveScores = true;
			double pvalue = Double.NaN;
			if (onlyTakePositiveScores) {
				if (realScore >= 0.0f) {
					pvalue = calculateProportionPositive(realScore, doubleScoreDistributions);
				} else {
					pvalue = calculateProportionNegative(realScore, doubleScoreDistributions);
				}
			} else {
				pvalue = calculateProportionAll(realScore, doubleScoreDistributions);
			}
			if (Double.isNaN(pvalue)) {
				System.out.println("asdf");
				int factor = 10;
				while (Double.isNaN(pvalue)) {
					factor += 10;
					// make the bins of the distribution smaller, so there is less gaps that can
					// make the value to fall into that gap and make pvalue to be NaN

					final int binCount = doubleScoreDistributions.size() / factor;
					if (binCount < 1) {
						break;
					}
//					final EmpiricalDistribution distribution = new EmpiricalDistribution(binCount);
//					distribution.load(doubleScoreDistributions.toArray());
//					pvalue = 1 - distribution.cumulativeProbability(realScore);
//					if (!Double.isNaN(pvalue)) {
//						logStatus(cellType.getName() + " with factor " + factor + " with bins "
//								+ distribution.getBinCount());
//					}
				}
			}
			cellType.setEnrichmentSignificance(pvalue);
		}
	}

	private double calculateProportionAll(float realScore, TFloatList scoreDistribution) {
		scoreDistribution.sort();
		int numHigherScores = 0;
		int total = 0;
		for (final float score : scoreDistribution.toArray()) {
			total++;
			if (score >= realScore) {
				numHigherScores++;
			}
		}
		final double ret = 1.0 * numHigherScores / total;
		return ret;
	}

	private double calculateProportionPositive(float realScore, TFloatList doubleScoreDistributions) {
		doubleScoreDistributions.sort();
		int numHigherScores = 0;
		int total = 0; // this total only counts the positive values
		for (final float score : doubleScoreDistributions.toArray()) {
			if (score <= 0f) {
				continue;
			}
			total++;
			if (score > realScore) {
				numHigherScores++;
			}
		}
		final double ret = 1.0 * numHigherScores / total;
		return ret;
	}

	private double calculateProportionNegative(float realScore, TFloatList doubleScoreDistributions) {
		doubleScoreDistributions.sort();
		int total = 0; // this total only counts the negative values
		int numLowerScores = 0;
		for (final float score : doubleScoreDistributions.toArray()) {

			if (score >= 0f) {
				continue;
			}
			total++;
			if (score < realScore) {
				numLowerScores++;
			}
		}
		final double ret = 1.0 * numLowerScores / total;
		return ret;
	}

	private void readRandomDistributionFile(File randomScoresFile, List<CellTypeClassification> cellTypeClassifications)
			throws IOException {
		PCTSEA.logStatus("File with random score distributions found: '"
				+ FilenameUtils.getName(randomScoresFile.getAbsolutePath()) + "'. Loading distributions now...");
		final Map<String, CellTypeClassification> cellTypeClassificationsPerName = new THashMap<String, CellTypeClassification>();
		cellTypeClassifications.forEach(c -> cellTypeClassificationsPerName.put(c.getName(), c));
		final BufferedReader reader = new BufferedReader(new FileReader(randomScoresFile));
		String line = null;

		int numLine = 0;
		while ((line = reader.readLine()) != null) {
			numLine++;
			if (numLine == 1) {
				continue;
			}
			final String[] split = line.split("\t");
			String cellType = split[0];
			if (!cellTypeClassificationsPerName.containsKey(cellType)) {
				cellType = SingleCell.parseCellTypeTypos(cellType);
			}

			final CellTypeClassification cellTypeClassification = cellTypeClassificationsPerName.get(cellType);
			if (cellTypeClassification == null) {
				continue;
			}
			// 0 cell type - 1 real score - 2 diff - 3 mean - 4 stdev -
			for (int i = 5; i < split.length; i = i + 2) {
				final float score = Float.valueOf(split[i]);
				final float dStatistic = Float.valueOf(split[i + 1]);
				cellTypeClassification.addRandomEnrichment(score, dStatistic);
			}
		}

		reader.close();
		PCTSEA.logStatus("Random score distributions loaded.");
	}

	/**
	 * 
	 * @param randomScoresFile
	 * @param randomScoreDistributionsPerCellType
	 * @throws IOException
	 */
	private void printToRandomDistributionFile(File randomScoresFile, List<CellTypeClassification> cellTypes)
			throws IOException {
		final FileWriter fw = new FileWriter(randomScoresFile);
		final BufferedWriter buffer = new BufferedWriter(fw);

		// header
		buffer.write("cell type\treal score\tdiff\tavg random score\tstd random score\trandom scores\n");

		Collections.sort(cellTypes, new Comparator<CellTypeClassification>() {

			@Override
			public int compare(CellTypeClassification o1, CellTypeClassification o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (final CellTypeClassification cellType : cellTypes) {
			final float realScore = cellType.getEnrichmentScore();
			buffer.write(cellType.getName() + "\t" + realScore + "\t");
			final TFloatList randomScores = cellType.getRandomEnrichmentScores();
			final TFloatList randomKSTestDStatistics = cellType.getRandomKSTestDStatistics();
			final float mean = Maths.mean(randomScores);
			final double stddev = Maths.stddev(randomScores);
			final double diff = realScore - mean;
			buffer.write(diff + "\t" + mean + "\t" + stddev);
			for (int i = 0; i < randomScores.size(); i++) {
				final double score = randomScores.get(i);
				final double dStatistic = randomKSTestDStatistics.get(i);
				buffer.write("\t" + score + "\t" + dStatistic);
			}
			buffer.write("\n");
		}
		buffer.close();
		PCTSEA.logStatus("File with null distributions of scores written at: " + randomScoresFile.getAbsolutePath());
	}

	private File getRandomScoresFile(ScoringMethod scoringMethod) {
		return new File(getCurrentTimeStampPath(scoringMethod) + prefix + "_" + maxIterations + "_random_scores.txt");
	}

	/**
	 * folder_where_iput_file_is/time_stamp/
	 * 
	 * @return
	 */
	private String getCurrentTimeStampPath(ScoringMethod scoringMethod) {
		return getCurrentTimeStampFolder(scoringMethod).getAbsolutePath() + File.separator;
	}

	/**
	 * folder_where_iput_file_is/time_stamp/
	 * 
	 * @return
	 */
	private File getCurrentTimeStampFolder(ScoringMethod scoringMethod) {
		String scoringMethodString = "";
		if (scoringMethod != null) {
			scoringMethodString = "_" + scoringMethod.getScoreName();
		}
		final String currenttimeStampPath = experimentExpressionFile.getParent() + File.separator + currentTimeStamp
				+ scoringMethodString + File.separator;
		final File file = new File(currenttimeStampPath);
		if (!currentTimeStampFolderCreated) {

			file.mkdirs();
			currentTimeStampFolderCreated = true;
		}
		return file;
	}

	/**
	 * Prints cell type classifications into a table in a file.
	 * 
	 * @param cellTypeClassifications
	 * @param singleCellList
	 * @param scoringSchema
	 * @param minNumberExpressedGenesInCell
	 * @throws IOException
	 */
	private void printCellTypeClassifications(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, ScoringSchema scoringSchema) throws IOException {
		logStatus("Printing output table...");
		// sort cell type classifications by the enrichment score significancy, but
		// keeping the negative scores at the end
		Collections.sort(cellTypeClassifications, new Comparator<CellTypeClassification>() {

			@Override
			public int compare(CellTypeClassification o1, CellTypeClassification o2) {
				if (o1.getNormalizedEnrichmentScore() > 0.0f && o2.getNormalizedEnrichmentScore() < 0.0f) {
					return -1;
				}

				if (o2.getNormalizedEnrichmentScore() > 0.0f && o1.getNormalizedEnrichmentScore() < 0.0f) {
					return 1;
				}
				// first by FDR
				int compare = Double.compare(o1.getEnrichmentFDR(), o2.getEnrichmentFDR());
				if (compare != 0) {
					return compare;
				}

				final double enrichmentSig1 = Double.isNaN(o1.getEnrichmentSignificance()) ? 1.0
						: o1.getEnrichmentSignificance();
				final double enrichmentSig2 = Double.isNaN(o2.getEnrichmentSignificance()) ? 1.0
						: o2.getEnrichmentSignificance();
				compare = Double.compare(enrichmentSig1, enrichmentSig2);
				if (compare != 0) {
					return compare;
				}
				// compare the normalized enrichment score, the higher the better
				final double enrichmentScore1 = Double.isNaN(o1.getNormalizedEnrichmentScore()) ? -1.0
						: o1.getNormalizedEnrichmentScore();
				final double enrichmentScore2 = Double.isNaN(o2.getNormalizedEnrichmentScore()) ? -1.0
						: o2.getNormalizedEnrichmentScore();
				compare = Double.compare(enrichmentScore2, enrichmentScore1);
				if (compare != 0) {
					return compare;
				}
				// compare the significance string
				String significance1 = o1.getSignificancyString() == null ? "" : o1.getSignificancyString();
				String significance2 = o2.getSignificancyString() == null ? "" : o2.getSignificancyString();
				if ("***".equals(significance1)) {
					significance1 = "0.001";
				} else if ("**".equals(significance1)) {
					significance1 = "0.01";
				} else if ("*".equals(significance1)) {
					significance1 = "0.05";
				} else {
					significance1 = "1";
				}
				if ("***".equals(significance2)) {
					significance2 = "0.001";
				} else if ("**".equals(significance2)) {
					significance2 = "0.01";
				} else if ("*".equals(significance2)) {
					significance2 = "0.05";
				} else {
					significance2 = "1";
				}
				compare = Double.compare(Double.valueOf(significance1), Double.valueOf(significance2));

				return compare;

			}
		});
		ConcurrentUtil.sleep(1L);
		final int numSingleCells = singleCellList.size();

		final long numSingleCellsPassingThreshold = scoringSchema.getScoringThreshold()
				.getCountSingleCellsPassingThreshold(singleCellList);

		// print to file
		final File parametersFile = getParametersOutputFile(scoringSchema.getScoringMethod());
		final File cellTypesFile = getCellTypesOutputFile(scoringSchema.getScoringMethod());
		FileWriter fw = null;
		BufferedWriter buffer = null;
		FileWriter fwParameters = null;
		try {
			fw = new FileWriter(cellTypesFile);
			buffer = new BufferedWriter(fw);
			fwParameters = new FileWriter(parametersFile);
			/////////////////
			// Header with parameters
			String parameters = getParametersString();
			parameters = parameters.replace(" = ", ":\t");
			final StringBuilder paramsHeader = new StringBuilder();
			paramsHeader.append("PCTSEA");
			final AppVersion version = AutomaticGUICreator.getVersion();
			if (version != null) {
				paramsHeader.append(" version:\t" + version.toString());
			}
			paramsHeader.append("\n");
			paramsHeader.append("Time:\t" + AutomaticGUICreator.getFormattedTime() + "\n");
			paramsHeader.append(parameters);
			paramsHeader.append(
					"Total number of single cells with at least " + scoringSchema.getMinNumberExpressedGenesInCell()
							+ " genes present in the input data:\t" + singleCellList.size() + "\n");
			buffer.write(paramsHeader.toString());
			fwParameters.write(paramsHeader.toString());
			fwParameters.close();

			////////////
			// glossary of some columns:
			final StringBuilder glossary = new StringBuilder("Glossary of columns:\n");
			glossary.append("hyperG_p-value column:\tp-value obtained from performing an hypergeometric test\n");
			glossary.append(
					"log2_ratio column:\tRatio of ratios between the ratio of # cells of type passing correlation threshold and # all cells of type, divided by the ratio between all # cells of type and # total cells (log2((cells of type core "
							+ sequentialScoringSchemas + "/cells core " + sequentialScoringSchemas
							+ ")/(cells of type/total cells))\n");
//		glossary.append(
//				"eus column:\tEnrichment Unweighted Score equal to the supremum of the differences between the unweigted cumulative distributions of the correlations of the cells of the cell type and the rest of the cells belonging to other cell types\n");
//		glossary.append(
//				"eus p-value column:\tEnrichment Unweighted Score associated p-value (calculated with Apache)\n");
			glossary.append(
					"ews column:\tEnrichment Weighted Score equal to the supremum of the differences between the weighted cumulative distributions of the correlations of the cell of the cell type and the rest of the cells belongind to other cell types\n");
			glossary.append("norm-ews column:\tEnrichment Weighted Score normalized by the sizes of the cell types\n");

			glossary.append("supX column:\tLocation of the supremum in the correlation-ranked cell list (x axis)\n");
			glossary.append(
					"norm-supX column:\tNormalized location of the supremum (from 0 to 1) in the correlation-ranked cell list (x axis)\n");
			glossary.append(
					"empirical_p-value column:\tSignificance probability of the Enrichment Weigthed Score based random permutations of cell types labels\n");
			glossary.append("FDR column:\tEnrichment False Discovery Rate\n");

			glossary.append(
					"2nd_ews column:\tSecondary Enrichment Weigthed Score, calculated from the second best supremum that is positive and is located in a higher ranking on the correlation-ranked cell list\n");
			glossary.append(
					"2nd_supX column:\tLocation of the second best supremum that is positive and has a higher ranking on the correlation-ranked cell list\n");
			glossary.append("size a (type) column:\tNumber of cells of type X that pass the correlation threshold.\n");
			glossary.append(
					"size_b_others column:\tNumber of cells NOT of type X that pass the correlation threshold.\n");
			glossary.append(
					"Dab_column:\tTwo-sample KS goodness-of-fit statistic. It is used for the two-sample KS goodness-of-fit significance calculation.\n");
			glossary.append("KS p-value column:\tTwo-sample KS goodness-of-fit p-value.\n");
			glossary.append(
					"KS_p-value_BH_corrected column:\tTwo-sample KS goodness-of-fit p-value corrected by Benjamini Hochberg (BH) method.\n");
			glossary.append(
					"KS_significance_level column:\tTwo-sample KS goodness-of-fit significance level. '***'-> significant at alpha 0.001, '**'-> significant at alpha 0.01,'*'-> significant at alpha 0.05\n");
			glossary.append(
					"Num_Genes_Significant:\tNumber of genes among the input list that are significantly expressed in the cell type (hyper-geometric test<0.001).\n");
			glossary.append(
					"Umap_x and Umap_y columns:\tCoordinates of the cell type after performing a Uniform Manifold Approximation and Projection (UMAP) clustering of all cell types with positive ews\n");
			glossary.append("'KS' term in this glossary:\tKolmogorov-Smirnov goodness-of-fit test\n");
			buffer.write(glossary.toString());

			////////////////////
			// Header of table
			buffer.write(CellTypesOutputTableColumns.getHeaderString("\t") + "\n");

			boolean positiveScore = true;
			for (final CellTypeClassification cellType : cellTypeClassifications) {
				if (positiveScore && cellType.getEnrichmentScore() < 0.0f) {
					buffer.write(NEGATIVE_EWS_FROM_HERE + "\n");
					positiveScore = false;
				}
				for (final CellTypesOutputTableColumns column : CellTypesOutputTableColumns.values()) {
					buffer.write(column.getValue(cellType, numSingleCells, numSingleCellsPassingThreshold,
							sequentialScoringSchemas) + "\t");
				}
				buffer.write("\n");
				buffer.flush();
			}
			logStatus("File created: " + FileNameUtils.getBaseName(cellTypesFile.getAbsolutePath()));
			log.info("File writen at " + cellTypesFile.getAbsolutePath());
		} finally {
			if (buffer != null) {
				buffer.close();
			}
		}
	}

	/**
	 * Returns an string with all parameters and values separated by ' = '
	 * 
	 * @return
	 */
	private String getParametersString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(InputParameters.EMAIL + " = " + email + "\n");
		sb.append(InputParameters.OUT + " = " + prefix + "\n");
		sb.append(InputParameters.PERM + " = " + maxIterations + "\n");
		sb.append(InputParameters.EEF + " = " + experimentExpressionFile.getAbsolutePath() + "\n");

		for (int i = 0; i < sequentialScoringSchemas.size(); i++) {
			final ScoringSchema scoringSchema = sequentialScoringSchemas.get(i);
			if (sequentialScoringSchemas.size() > 1) {
				sb.append("Round " + (i + 1) + "\n");
			}
			sb.append(InputParameters.SCORING_METHOD + " = " + scoringSchema.getScoringMethod().getScoreName() + "\n");
			sb.append(
					InputParameters.MIN_SCORE + " = " + scoringSchema.getScoringThreshold().getThresholdValue() + "\n");

			sb.append(
					InputParameters.MIN_GENES_CELLS + " = " + scoringSchema.getMinNumberExpressedGenesInCell() + "\n");
		}

		sb.append(InputParameters.CELL_TYPES_CLASSIFICATION + " = " + cellTypeBranch + "\n");
		sb.append(InputParameters.LOAD_RANDOM + " = " + loadRandomDistributionsIfExist + "\n");
		sb.append(InputParameters.PLOT_NEGATIVE_ENRICHED + " = " + plotNegativeEnrichedCellTypes + "\n");
		sb.append(InputParameters.WRITE_SCORES + " = " + writeScoresFile + "\n");
		sb.append(InputParameters.UNIPROT_RELEASE + " = " + uniprotRelease + "\n");

		return sb.toString();
	}

	private List<CellTypeClassification> calculateHyperGeometricStatistics(List<SingleCell> singleCellList,
			ScoreThreshold scoreThreshold, ScoringMethod scoringMethod) {
		PCTSEA.logStatus("Starting hyperG...");
		final List<Integer> celltypes = singleCellList.stream().map(sc -> sc.getCellTypeID())
				.filter(ct -> !ct.equals(-1)).distinct().sorted().collect(Collectors.toList());
		final String message = celltypes.size() + " different cell types with branch " + cellTypeBranch;
		PCTSEA.logStatus(message);

		// by cell types

		final int numSingleCells = singleCellList.size();

		final List<SingleCell> singleCellsPassingThreshold = scoreThreshold
				.getSingleCellsPassingThreshold(singleCellList);
		final int numSingleCellsPassingThreshold = singleCellsPassingThreshold.size();
		PCTSEA.logStatus(numSingleCellsPassingThreshold + " single cells with " + scoringMethod.getScoreName()
				+ " passing threshold " + scoreThreshold);
		PCTSEA.logStatus("Calculating hypergeometric statistics...");
		//
		int N; // population size
		int K; // successes in entire population
		int n; // sample size, number of draws
		int k; // observed successes in sample
		//
		int numSignificantCellTypes = 0;
		N = numSingleCells;
		final List<CellTypeClassification> newCellTypeClassifications = new ArrayList<CellTypeClassification>();
		for (final Integer cellTypeID : celltypes) {
			ConcurrentUtil.sleep(1L);
			// cells of type
			final List<SingleCell> cellsOfCellType = singleCellList.stream()
					.filter(sc -> sc.getCellTypeID() == cellTypeID).collect(Collectors.toList());
			final int numCellsOfType = cellsOfCellType.size();

			// in the first approach (Salva's):<br>
			// N = population size
			// K = successes in the population
			// n = sample size
			// k = successes in the sample
			//
			// K = number of single cells that have a positive correlation with the
			// interactors
			// n = number of single cells of type X
			// k = number of single cells that have a positive correlation with the
			// interactors and that are of type X

			final int numCellsOfTypeWithPositiveCorrelation = (int) scoreThreshold
					.getCountSingleCellsPassingThreshold(cellsOfCellType);

			K = numSingleCellsPassingThreshold;
			n = numCellsOfType;
			k = numCellsOfTypeWithPositiveCorrelation;
			final HypergeometricDistribution hg = new HypergeometricDistribution(N, K, n);
			double p = hg.upperCumulativeProbability(k);
			if (p < 0.0) {
				p = 0.0;
			}
			if (p < 0.05) {
				numSignificantCellTypes++;
			}
			// in the second approach (Casimir's):<br>
			// K = number of single cells of type X (number of successes)
			// n = number of single cells that have a positive correlation with the
			// interactors (sample size)
			// k = number of single cells that have a positive correlation with the
			// interactors and that are of type X (number of successes in sample)
			K = numCellsOfType;
			n = numSingleCellsPassingThreshold;
			k = numCellsOfTypeWithPositiveCorrelation;
			final HypergeometricDistribution hg2 = new HypergeometricDistribution(N, K, n);
			double p2 = hg2.upperCumulativeProbability(k);
			if (p2 < 0.0) {
				p2 = 0.0;
			}
//			System.out.println(p + "\t" + p2);
			final String cellTypeName = CellTypes.getCellTypeNameByCellTypeID(cellTypeID);
			final CellTypeClassification cellTypeClassification = new CellTypeClassification(cellTypeName, p);
			cellTypeClassification.setNumCellsOfType(numCellsOfType);
			cellTypeClassification.setNumCellsOfTypePassingCorrelationThreshold(numCellsOfTypeWithPositiveCorrelation);
			cellTypeClassification.setSingleCells(cellsOfCellType);

			// casimir's enrichment score
			// log2((cells of type core >0.1/cells core >0.1)/(cells of type/total cells))

			if (numCellsOfType > 0 && numSingleCellsPassingThreshold > 0) {
				final double x = (1.0 * numCellsOfTypeWithPositiveCorrelation / numSingleCellsPassingThreshold)
						/ (1.0 * numCellsOfType / numSingleCells);
				final Double casimirsEnrichmentScore = Maths.log(x, 2);
				cellTypeClassification.setCasimirsEnrichmentScore(casimirsEnrichmentScore.floatValue());
			} else {
				PCTSEA.logStatus(cellTypeName + " cannot have a casimirs score because # of '" + cellTypeName
						+ "' cells is " + numCellsOfType + " and # of '" + cellTypeName + "' cells with score "
						+ scoreThreshold + " is " + numSingleCellsPassingThreshold);
			}
			newCellTypeClassifications.add(cellTypeClassification);
		}
		PCTSEA.logStatus("Hypergeometric statistics calculated. " + numSignificantCellTypes
				+ " cell types are significative (pvalue<0.05)");
		return newCellTypeClassifications;
	}

	/**
	 * Calculate the score (correlation or Morpheus-like score, or dot-product)
	 * between the expression of the experimental proteins and the single cells
	 * 
	 * @param singleCellList
	 * @param interactorExpressions
	 * @param scoreThreshold
	 * @param writeScoresFile
	 * @param outputToLog
	 * @param getExpressionsUsedForScore
	 * @param takeZerosForCorrelation
	 * @return the number of cells that pass the correlation threshold
	 * @throws IOException
	 */
	private int calculateScoresToRankSingleCells(List<SingleCell> singleCellList,
			InteractorsExpressionsRetriever interactorExpressions, ScoringSchema scoreSchema, boolean writeScoresFile,
			boolean outputToLog, boolean getExpressionsUsedForScore, boolean takeZerosForCorrelation)
			throws IOException {
		final ScoringMethod scoringMethod = scoreSchema.getScoringMethod();
		final File scoresOutputFile = getScoresOutputFile(scoringMethod);
		final int originalNumCells = singleCellList.size();
		int numPassingThreshold = 0;
		if (outputToLog) {
			PCTSEA.logStatus("Calculating " + scoringMethod.getScoreName() + "...");
		}
		// output correlations
		FileWriter scoreFileWriter = null;
		try {
			if (writeScoresFile) {
				scoreFileWriter = new FileWriter(scoresOutputFile);
				scoreFileWriter.write("cell ID\tcell_type\t" + scoringMethod.getScoreName()
						+ "\tvalues_used\tgenes\tnum_genes\tgene_expression_variance_on_cell\n");
			}
			final Iterator<SingleCell> cellsIterator = singleCellList.iterator();
			final ProgressCounter counter = new ProgressCounter(originalNumCells, ProgressPrintingType.PERCENTAGE_STEPS,
					0, true);
			counter.setSuffix("calculating " + scoringMethod.getScoreName());
			while (cellsIterator.hasNext()) {
				counter.increment();
				final String printIfNecessary = counter.printIfNecessary();
				if (!"".equals(printIfNecessary)) {
					ConcurrentUtil.sleep(1L);
					logStatus(printIfNecessary, false);
				}
				final SingleCell singleCell = cellsIterator.next();

				switch (scoringMethod) {
				case PEARSONS_CORRELATION:
					singleCell.calculateCorrelation(interactorExpressions, takeZerosForCorrelation,
							scoreSchema.getMinNumberExpressedGenesInCell(), getExpressionsUsedForScore);
					break;
				case SIMPLE_SCORE:
					singleCell.calculateSimpleScore(interactorExpressions, takeZerosForCorrelation,
							scoreSchema.getMinNumberExpressedGenesInCell(), getExpressionsUsedForScore);
					break;
				case DOT_PRODUCT:
					singleCell.calculateDotProductScore(interactorExpressions, takeZerosForCorrelation,
							scoreSchema.getMinNumberExpressedGenesInCell(), getExpressionsUsedForScore);
					break;

				default:
					throw new IllegalArgumentException(
							"Method " + scoringMethod.getScoreName() + " still not supported.");

				}

//			if (Double.isNaN(singleCell.getGeneExpressionVariance())
//					|| singleCell.getGeneExpressionVariance() < minCellsPerCellTypeForPDF) {
//				numCellsDiscardedByMinimumVariance++;
//				cellsIterator.remove();
//				continue;
//			}
				if (scoreSchema.getScoringThreshold().passThreshold(singleCell)) {
					numPassingThreshold++;
				}
			}
			logStatus("Scores (" + scoringMethod.getScoreName()
					+ ") between single cell expressions and input data are calculated.");

			ProgressCounter counter2 = null;
			if (writeScoresFile) {
				logStatus("Writting " + scoringMethod.getScoreName() + " to file...");
				counter2 = new ProgressCounter(singleCellList.size(), ProgressPrintingType.PERCENTAGE_STEPS, 0, true);
			}

			// we sort the single cell list to have them sorted by correlation
			scoreSchema.getScoringThreshold().sortSingleCellsByScore(singleCellList);

			// print to file and create chart
			final TDoubleList scores = new TDoubleArrayList();
			ConcurrentUtil.sleep(1L);

			for (final SingleCell singleCell : singleCellList) {
				if (!Double.isNaN(singleCell.getScoreForRanking())) {
					scores.add(singleCell.getScoreForRanking());
				}
				if (writeScoresFile) {
					counter2.increment();
					final String printIfNecessary = counter2.printIfNecessary();
					if (!"".equals(printIfNecessary)) {
						logStatus(printIfNecessary);
					}
					scoreFileWriter.write(singleCell.getCellTypeID() + "\t" + singleCell.getCellType() + "\t"
							+ +singleCell.getScoreForRanking() + "\t" + singleCell.getExpressionsUsedForScore() + "\t"
							+ singleCell.getGenesUsedForScoreString() + "\t" + singleCell.getGenesUsedForScore().size()
							+ "\t" + singleCell.getGeneExpressionVariance() + "\n");
				}
			}
			if (numPassingThreshold > 0) {
				createWholeDatasetScoreDistributionChart(scores, scoringMethod);
				if (writeScoresFile) {
					logStatus(scoringMethod.getScoreName()
							+ " written to file single cell expressions and input data are done.");
				}
			}
			return numPassingThreshold;
		} finally {
			if (writeScoresFile) {
				scoreFileWriter.close();
			}
			if (outputToLog) {
				if (!(scoreSchema.getScoringThreshold() instanceof NoThreshold)) {
					logStatus(numPassingThreshold + " cells pass the score threshold ("
							+ scoreSchema.getScoringThreshold() + ") out of " + originalNumCells);
				}
				if (writeScoresFile) {
					logStatus("File with " + scoringMethod.getScoreName() + " created at: "
							+ scoresOutputFile.getAbsolutePath());
				}
			}
		}

	}

	private void createWholeDatasetScoreDistributionChart(TDoubleList scores, ScoringMethod scoringMethod) {

		// create chart
//		final HistogramDataset histogramDataset = new HistogramDataset();
//
//		histogramDataset.addSeries(scores.size() + " single cells", scores.toArray(), 50);
//		final String plotTitle = scoringMethod.getScoreName() + " distribution";
//		final String xaxis = scoringMethod.getScoreName();
//		final String yaxis = "Frequency (# of cells)";
//		final PlotOrientation orientation = PlotOrientation.VERTICAL;
//		final boolean show = true;
//		final boolean toolTips = false;
//		final boolean urls = false;
//		final JFreeChart chart = ChartFactory.createXYLineChart(plotTitle, xaxis, yaxis, histogramDataset, orientation,
//				show, toolTips, urls);

		try {
			writeGlobalScoreDistributionChart(scores, scoringMethod);
//			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);

		} catch (final IOException e) {
			PCTSEA.logStatus("Some error occurred while creating chart for " + scoringMethod.getScoreName() + ": "
					+ e.getMessage(), LogLevel.ERROR);
		}

	}

	private void writeGlobalScoreDistributionChart(TDoubleList scores, ScoringMethod scoringMethod) throws IOException {
		final File resultsSubfolderGeneral = getResultsSubfolderGeneral(scoringMethod);

		final File outputTXTFile = PCTSEAUtils.getOutputTXTFile(resultsSubfolderGeneral, "corr_hist", prefix);
		final FileWriter fw = new FileWriter(outputTXTFile);
		final BufferedWriter buffer = new BufferedWriter(fw);
		buffer.write(scoringMethod.getScoreName() + "\n");
		for (final double score : scores.toArray()) {
			buffer.write(score + "\n");
		}
		buffer.close();
	}

	/**
	 * 
	 * @param cellTypeClassifications
	 * @param singleCellList                 cells filtered already by correlation
	 *                                       threshold
	 * @param calculateUnweighted
	 * @param calculateKolmogorovSmirnovTest
	 * @param outputToLog
	 * @param permutatedData
	 * @param plotNegativeEnrichedCellTypes
	 * @param scoreName
	 */
	private void calculateEnrichmentScore(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, boolean calculateUnweighted, boolean outputToLog, boolean permutatedData,
			boolean plotNegativeEnrichedCellTypes, ScoringMethod scoringMethod) {
		if (outputToLog) {
			PCTSEA.logStatus("Calculating enrichment scores...");
		}
		// first set NaN correlations to zero
		singleCellList.stream().forEach(s -> {
			if (Double.isNaN(s.getScoreForRanking())) {
				s.setScore(0.0);
			}
		});

		// calculate unweighted Score
		if (calculateUnweighted) {
			calculateUnweigthedEnrichmentScore(cellTypeClassifications, singleCellList, outputToLog);
		}
		// calculate weighted Score
		calculateWeigthedEnrichmentScoreInParallel(cellTypeClassifications, singleCellList, outputToLog, permutatedData,
				plotNegativeEnrichedCellTypes, scoringMethod);

		// sort by score descending
		Collections.sort(cellTypeClassifications, new Comparator<CellTypeClassification>() {

			@Override
			public int compare(CellTypeClassification o1, CellTypeClassification o2) {
				return Double.compare(o2.getEnrichmentScore(), o1.getEnrichmentScore());
			}
		});

//		if (calculateKolmogorovSmirnovTest) {
//			// calculate significance
//			calculateKolmogorovSmirnovTestInParallel(cellTypeClassifications, outputToLog);
//		}
	}

	private void calculateWeigthedEnrichmentScoreInParallel(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, boolean outputToLog, boolean permutatedData,
			boolean plotNegativeEnrichedCellTypes, ScoringMethod scoreMethod) {
		if (outputToLog) {
			PCTSEA.logStatus("Calculating weigthed enrichment score and KS statistics...");
		}
		// sort by score from higher to lower
		PCTSEAUtils.sortByScoreDescending(singleCellList);
		final ParIterator<CellTypeClassification> iterator = ParIteratorFactory
				.createParIterator(cellTypeClassifications, threadCount, Schedule.GUIDED);
		final List<EnrichmentWeigthedScoreParallel> runners = new ArrayList<EnrichmentWeigthedScoreParallel>();
		for (int numCore = 1; numCore <= threadCount; numCore++) {
			// take current DB session
			final EnrichmentWeigthedScoreParallel runner = new EnrichmentWeigthedScoreParallel(iterator, numCore,
					singleCellList, permutatedData, plotNegativeEnrichedCellTypes, scoreMethod.getScoreName(),
					getResultsSubfolderForCellTypes(scoreMethod), prefix, true);
			runners.add(runner);
			runner.start();
		}

		// Main thread waits for worker threads to complete
		for (int k = 0; k < threadCount; k++) {
			try {
				final EnrichmentWeigthedScoreParallel enrichmentWeigthedScoreParallel = runners.get(k);
				enrichmentWeigthedScoreParallel.join();

			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		// now we save the charts
		if (!permutatedData) {
//			saveScoreCalculationCharts(cellTypeClassifications, generatePDFCharts);
		}
	}

	private void saveScoreCalculationCharts(List<CellTypeClassification> cellTypeClassifications,
			boolean generatePDFCharts, ScoringMethod scoringMethod) {

		final List<CellTypeClassification> newList = new ArrayList<CellTypeClassification>();
		newList.addAll(cellTypeClassifications);

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				PCTSEA.logStatus("Creating charts for each cell type in the background...");
				final File resultsSubfolderForCellTypes = getResultsSubfolderForCellTypes(scoringMethod);

				// we create the writableImages on the FX thread and store them in a list, so
				// that then we can save them
				for (final CellTypeClassification cellTypeClassification : newList) {
//					try {
////						cellTypeClassification.saveCharts(resultsSubfolderForCellTypes, prefix, generatePDFCharts);
//					} catch (final IOException e) {
//						e.printStackTrace();
//						PCTSEA.logStatus("Some error occurred while saving chart for "
//								+ cellTypeClassification.getName() + ": " + e.getMessage(), LogLevel.ERROR);
//					}
				}
			}
		};
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		PCTSEA.this.savingFiles = executor.submit(runnable);
	}

	/**
	 * 
	 * @param cellTypeClassifications
	 * @param singleCellList
	 * @param outputToLog
	 */
	private void calculateUnweigthedEnrichmentScore(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, boolean outputToLog) {
		if (outputToLog) {
			PCTSEA.logStatus("Calculating unweigthed enrichment score...");
		}
		final long n = singleCellList.size();
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			double score = -Double.MAX_VALUE;
			final int cellTypeID = cellType.getCellTypeID();
			final long nk = singleCellList.stream().filter(c -> cellTypeID == c.getCellTypeID()).count();
			if (nk == 0) {
				cellType.setEnrichmentUnweightedScore(0.0f);
				continue;
			}
			final double denominatorA = Double.valueOf(nk);
			final double denominatorB = Double.valueOf(n - nk);
			double previousA = 0.0;
			double previousB = 0.0;
			int numeratorA = 0;
			int numeratorB = 0;
			final TDoubleArrayList distA = new TDoubleArrayList();
			final TDoubleArrayList distB = new TDoubleArrayList();
			for (final SingleCell singleCell : singleCellList) {
				double a = 0.0;
				double b = 0.0;

				if (cellTypeID == singleCell.getCellTypeID()) {
					numeratorA++;
					a = 1.0 * numeratorA / denominatorA;
					distA.add(singleCell.getScoreForRanking());
					b = previousB;
				} else {
					numeratorB++;
					a = previousA;
					b = 1.0 * numeratorB / denominatorB;
					distB.add(singleCell.getScoreForRanking());
				}
				final double difference = Math.abs(a - b);
				if (score < difference) {
					score = difference;
				}
				previousA = a;
				previousB = b;
			}
			if (distA.size() > 1 && distB.size() > 1) {
				final KolmogorovSmirnovTest test = new KolmogorovSmirnovTest();
				final Double kolmogorovSmirnovStatistic = test.kolmogorovSmirnovStatistic(distA.toArray(),
						distB.toArray());
				// now we apply a term factor which is coming from the equation 11 at
				// https://www.pathwaycommons.org/guide/primers/data_analysis/gsea/
				// but also from wikipedia at
				// https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test
				// when samples are large
//				final double sizea = distA.size();
//				final double sizeb = distB.size();
//				final double sqrt = Math.sqrt((sizea * sizeb) / (sizea + sizeb));
//				kolmogorovSmirnovStatistic = kolmogorovSmirnovStatistic * sqrt;
				cellType.setEnrichmentUnweightedScore(kolmogorovSmirnovStatistic.floatValue());
			} else {
				cellType.setEnrichmentUnweightedScore(Float.NaN);
			}
//			cellType.setEnrichmentUnweightedScore(score);

		}

	}

	private File getResultsSubfolderGeneral(ScoringMethod scoringMethod) {
		if (resultsSubFolderGeneral == null) {
			resultsSubFolderGeneral = new File(getCurrentTimeStampPath(scoringMethod) + "global_charts");
			if (!resultsSubFolderGeneral.exists()) {
				resultsSubFolderGeneral.mkdirs();
			}
		}
		return resultsSubFolderGeneral;
	}

	private File getResultsSubfolderForCellTypes(ScoringMethod scoringMethod) {
		if (resultsSubFolderForCellTypes == null) {
			resultsSubFolderForCellTypes = new File(getCurrentTimeStampPath(scoringMethod) + "cell_types_charts");
			if (!resultsSubFolderForCellTypes.exists()) {
				resultsSubFolderForCellTypes.mkdirs();
			}
		}
		return resultsSubFolderForCellTypes;
	}

	private File getZipOutputFile(ScoringMethod scoringMethod) {

		final File currentTimeStampFolder = getCurrentTimeStampFolder(scoringMethod);
		File file = new File(currentTimeStampFolder.getParent() + File.separator
				+ FilenameUtils.getBaseName(currentTimeStampFolder.getAbsolutePath()) + ".zip");
		if (file.exists()) {
			int i = 2;
			while (file.exists()) {
				file = new File(currentTimeStampFolder.getParent() + File.separator
						+ FilenameUtils.getBaseName(currentTimeStampFolder.getAbsolutePath()) + "_" + i + ".zip");
				i++;
			}
		}
		return file;
	}

	private File getGenesInvolvedInCorrelationsOutputFile(ScoringMethod scoringMethod) {
		return new File(
				getCurrentTimeStampPath(scoringMethod) + prefix + "_" + scoringMethod.getScoreName() + "_genes.txt");
	}

	private File getCellTypesOutputFile(ScoringMethod scoringMethod) {
		return new File(getCurrentTimeStampPath(scoringMethod) + prefix + "_cell_types_enrichment.txt");
	}

	private File getParametersOutputFile(ScoringMethod scoringMethod) {
		return new File(getCurrentTimeStampPath(scoringMethod) + prefix + "_parameters.txt");
	}

	private File getScoresOutputFile(ScoringMethod scoringMethod) {
		final String scoreName = scoringMethod.getScoreName();
		return new File(getCurrentTimeStampPath(scoringMethod) + prefix + "_single_cell_" + scoreName + ".txt");
	}

	private File getGeneExpressionOutputFile(String geneName, ScoreThreshold scoreThreshold2,
			ScoringMethod scoringMethod) {
		return new File(getCurrentTimeStampPath(scoringMethod) + prefix + "_" + geneName + "_expressions_with_"
				+ scoringMethod.getScoreName() + "_" + scoreThreshold2.getThresholdValue() + ".txt");
	}

	public void setPrefix(String prefix) {
		if (prefix != null) {
			final String tmp = FileUtils.checkInvalidCharacterNameForFileName(prefix);
			if (!prefix.equals(tmp)) {
				throw new IllegalArgumentException("Prefix contains invalid characters");
			}
		}
		this.prefix = prefix;
	}

	public void setExperimentExpressionFile(File experimentExpressionFile2) {
		experimentExpressionFile = experimentExpressionFile2;
	}

	public void addScoreSchema(ScoringSchema scoreSchema) {
		sequentialScoringSchemas.add(scoreSchema);
	}

	public void setLoadRandomDistributionsIfExist(boolean loadRandomDistributionsIfExist2) {
		loadRandomDistributionsIfExist = loadRandomDistributionsIfExist2;
	}

	public void setMaxIterations(int maxIterations2) {
		maxIterations = maxIterations2;
	}

	public void setCellTypesBranch(CellTypeBranch cellTypeBranch2) {
		cellTypeBranch = cellTypeBranch2;

	}

	public void setPlotNegativeEnrichedCellTypes(boolean plotNegativeEnrichedCellTypes2) {
		plotNegativeEnrichedCellTypes = plotNegativeEnrichedCellTypes2;
	}

	public void setStatusListener(StatusListener<Boolean> statusListener) {
		PCTSEA.statusListener = statusListener;
	}

	/**
	 * Log in {@link LogLevel} info level by default to the {@link StatusListener}
	 * and to the app logging system
	 * 
	 * @param message
	 */
	public static void logStatus(String message) {
		logStatus(message, LogLevel.INFO);
	}

	public static void logStatus(String message, boolean inNewLine) {
		logStatus(message, LogLevel.INFO, inNewLine);
	}

	public static void logStatus(String message, LogLevel level) {
		logStatus(message, level, true);
	}

	/**
	 * Log to {@link StatusListener} and to the app logging system
	 * 
	 * @param message
	 * @param level
	 * @param inNewLine
	 */
	public static void logStatus(String message, LogLevel level, boolean inNewLine) {
		if (statusListener != null) {
			statusListener.onStatusUpdate(
//					format.format(new Date()) + ": " + 
					message, inNewLine);
		}
		switch (level) {
		case DEBUG:
			log.debug(message);
			break;
		case ERROR:
			log.error(message);
			break;
		case INFO:
			log.info(message);
			break;
		case TRACE:
			log.trace(message);
			break;
		case WARN:
			log.warn(message);
			break;
		default:
			log.debug(message);
			break;
		}
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setDataset(Dataset dataset2) {
		dataset = dataset2;
	}

	public boolean isWriteCorrelationsFile() {
		return writeScoresFile;
	}

	public void setWriteCorrelationsFile(boolean writeCorrelationsFile) {
		writeScoresFile = writeCorrelationsFile;
	}

	public void setUniprotRelease(String uniprotRelease) {
		this.uniprotRelease = uniprotRelease;
	}

	public void setInputDataType(InputDataType inputDataType2) {
		inputDataType = inputDataType2;
	}
}
