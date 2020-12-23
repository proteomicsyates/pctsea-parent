package edu.scripps.yates.pctsea;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

import com.google.common.io.Files;

import edu.scripps.yates.pctsea.correlation.CorrelationThreshold;
import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLog;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.model.GeneOccurrence;
import edu.scripps.yates.pctsea.model.InputParameters;
import edu.scripps.yates.pctsea.model.PCTSEAResult;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.model.charts.ChartsGenerated;
import edu.scripps.yates.pctsea.model.charts.DoubleCategoryItemLabelGenerator;
import edu.scripps.yates.pctsea.model.charts.IntegerCategoryItemLabelGenerator;
import edu.scripps.yates.pctsea.model.charts.LabelGenerator;
import edu.scripps.yates.pctsea.model.charts.LabeledXYDataset;
import edu.scripps.yates.pctsea.utils.EmailUtil;
import edu.scripps.yates.pctsea.utils.PCTSEAUtils;
import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import edu.scripps.yates.pctsea.utils.parallel.EnrichmentWeigthedScoreParallel;
import edu.scripps.yates.pctsea.utils.parallel.KolmogorovSmirnovTestParallel;
import edu.scripps.yates.utilities.appversion.AppVersion;
import edu.scripps.yates.utilities.cores.SystemCoreManager;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.files.ZipManager;
import edu.scripps.yates.utilities.maths.Histogram;
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
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.THashSet;
import tagbio.umap.Umap;
import tagbio.umap.metric.EuclideanMetric;

public class PCTSEA {

	private final ExpressionMongoRepository expressionMongoRepo;
	private final DatasetMongoRepository datasetMongoRepo;
	private final SingleCellMongoRepository singleCellMongoRepo;
	private final PctseaRunLogRepository runLogsRepo;
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
	private CorrelationThreshold correlationThreshold;
	private int minNumberExpressedGenesInCell;
	private final boolean takeZerosForCorrelation = false;
	private String prefix;

	private int maxIterations;
	private boolean loadRandomDistributionsIfExist;
	private static final int threadCount = SystemCoreManager.getAvailableNumSystemCores();
	private CellTypeBranch cellTypeBranch;
	private InteractorsExpressionsRetriever interactorExpressions;
	protected Future<?> savingFiles;
	private boolean generatePDFCharts;
	private int minCellsPerCellTypeForPDF;
	private boolean plotNegativeEnrichedCellTypes;

	private static StatusListener statusListener;

	private String currentTimeStamp;

	private String email;

	private Dataset dataset;

	private String resultsViewerURL;

	private boolean writeCorrelationsFile = false;

	private String fromEmail;

	public PCTSEA(InputParameters inputParameters, ExpressionMongoRepository expressionMongoRepo,
			SingleCellMongoRepository singleCellMongoRepo, PctseaRunLogRepository runLogsRepo,
			DatasetMongoRepository datasetMongoRepo, MongoBaseService mongoBaseService) {

		this.expressionMongoRepo = expressionMongoRepo;
		this.singleCellMongoRepo = singleCellMongoRepo;
		this.mongoBaseService = mongoBaseService;
		this.datasetMongoRepo = datasetMongoRepo;
		this.runLogsRepo = runLogsRepo;
		correlationThreshold = new CorrelationThreshold(inputParameters.getMinCorrelation());
		cellTypeBranch = inputParameters.getCellTypesClassification();
		experimentExpressionFile = new File(inputParameters.getInputDataFile());
		generatePDFCharts = inputParameters.isGeneratePDFCharts();
		loadRandomDistributionsIfExist = inputParameters.isLoadRandom();
		maxIterations = inputParameters.getNumPermutations();
		minCellsPerCellTypeForPDF = inputParameters.getMinCellsPerCellType();
		minNumberExpressedGenesInCell = inputParameters.getMinGenesCells();
		writeCorrelationsFile = inputParameters.isWriteCorrelationsFile();
		email = inputParameters.getEmail();
		dataset = inputParameters.getDataset();
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
			MongoBaseService mongoBaseService) {
		this.expressionMongoRepo = expressionMongoRepo;
		this.singleCellMongoRepo = singleCellMongoRepo;
		this.mongoBaseService = mongoBaseService;
		this.runLogsRepo = runLogsRepo;
		this.datasetMongoRepo = datasetMongoRepo;

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
		logStatus("Looking for dataset in DB with tag '" + getInputParameters().getDataset().getTag() + "'...");
		// check dataset input parameters
		final List<Dataset> datasetFromDB = datasetMongoRepo.findByTag(getInputParameters().getDataset().getTag());
		logStatus(datasetFromDB.size() + " datasets in DB with tag'" + getInputParameters().getDataset().getTag());
		if (datasetFromDB == null || datasetFromDB.isEmpty()) {
			final List<String> datasetTags = datasetMongoRepo.findAll().stream().map(dataset -> dataset.getTag())
					.sorted().collect(Collectors.toList());
			throw new IllegalArgumentException("Dataset " + getInputParameters().getDataset().getTag()
					+ " doesn't exist in DB. Available datasets are: "
					+ StringUtils.getSortedSeparatedValueStringFromChars(datasetTags, ","));
		}
		// first of all create a time stamp
		currentTimeStamp = createTimeStamp(prefix);
		// create log
		final PctseaRunLog runLog = new PctseaRunLog();
		runLog.setTimeStamp(currentTimeStamp);
		runLog.setStarted(getDateNow());
		runLog.setInputParameters(getInputParameters());
		// save to repo
		runLogsRepo.save(runLog);

		Exception errorMessage = null;
		final File zipOutputFile = getZipOutputFile();

		// create PCTSEAResult object
		PCTSEAResult result = null;
		try {
			URL urlToViewer = null;
			if (resultsViewerURL != null) {
				urlToViewer = new URL(
						resultsViewerURL + "/?results=" + FilenameUtils.getBaseName(zipOutputFile.getAbsolutePath()));
			}
			result = new PCTSEAResult(zipOutputFile, urlToViewer, runLog);
			if (generatePDFCharts) {
				ChartsGenerated.getNewInstance();
			}
//			// read input files
//			final SingleCellsMetaInformationReader cellsInfo = new SingleCellsMetaInformationReader(
//					this.singleCellsMetadataFile);
//			final List<SingleCell> singleCellList = cellsInfo.getSingleCellList();
//			updateSingleCellsType(cellsInfo, this.expressionMongoRepo);
//			if (true) {
//				return;
//			}

			final List<SingleCell> singleCellList = getSingleCellListFromDB(dataset);

			interactorExpressions = new InteractorsExpressionsRetriever(expressionMongoRepo, mongoBaseService,
					experimentExpressionFile, dataset);
			// log
			runLog.setNumInputGenes(interactorExpressions.getInteractorsGeneIDs().size());

			// calculate correlations
			final int numCellsPassingCorrelationThreshold = correlateSingleCellsToInteractors(singleCellList,
					interactorExpressions, correlationThreshold, cellTypeBranch, writeCorrelationsFile, true, true,
					takeZerosForCorrelation, minNumberExpressedGenesInCell);
			if (numCellsPassingCorrelationThreshold == 0) {
				throw new IllegalArgumentException(
						"There is not any cell passing the minimum correlation threshold " + correlationThreshold);
			}

			// discard single cells that have negative correlation
			final Iterator<SingleCell> iterator = singleCellList.iterator();
			while (iterator.hasNext()) {

				final SingleCell cell = iterator.next();
				if (cell.getCorrelation() < 0) {
					iterator.remove();
				}
			}
			ConcurrentUtil.sleep(1L);
			PCTSEA.logStatus(singleCellList.size() + " single cells have positive correlations (> 0)");

			// make a chart with the histogram of number of genes used to correlate for each
			// cells
			createHistogramOfCorrelatingGenes(singleCellList, null);
			createHistogramOfCorrelatingGenes(singleCellList, correlationThreshold.getThresholdValue());
			// make a chart with the distribution of correlations over the ranked list of
			// cells
			createDistributionOfCorrelationsOverRankedCells(singleCellList, correlationThreshold);

			ConcurrentUtil.sleep(1L);
			// calculate hypergeometric statistics
			final List<CellTypeClassification> cellTypeClassifications = calculateHyperGeometricStatistics(
					singleCellList, cellTypeBranch, correlationThreshold);

			// calculate enrichment scores with the Kolmogorov-Smirnov test
			final List<SingleCell> singleCellsPassingCorrelationThreshold = correlationThreshold
					.getSingleCellsPassingThresholdSortedByCorrelation(singleCellList);

			///////////////////////////////////////////////
			// REQUEST FROM CASIMIR FROM EMAIL ON Nov 3, 2020 8:22am
			printGeneExpression(singleCellsPassingCorrelationThreshold, "ACE2", correlationThreshold);

			///////////////////////////////////////////////
			calculateEnrichmentScore(cellTypeClassifications, singleCellsPassingCorrelationThreshold, cellTypeBranch,
					true, false, true, false, generatePDFCharts, minCellsPerCellTypeForPDF,
					plotNegativeEnrichedCellTypes);

			// calculate significance by cell types permutations
			calculateSignificanceByCellTypesPermutations(interactorExpressions, cellTypeClassifications,
					singleCellsPassingCorrelationThreshold, cellTypeBranch, maxIterations,
					loadRandomDistributionsIfExist, minCellsPerCellTypeForPDF, plotNegativeEnrichedCellTypes);

			// calculate significance by phenotype permutations
			if (false) { // DISABLED since we used significance by cell types permutations
				calculateSignificanceByPhenotypePermutations(interactorExpressions, cellTypeClassifications,
						singleCellsPassingCorrelationThreshold, cellTypeBranch, correlationThreshold,
						loadRandomDistributionsIfExist, generatePDFCharts, minCellsPerCellTypeForPDF,
						plotNegativeEnrichedCellTypes, maxIterations, takeZerosForCorrelation,
						minNumberExpressedGenesInCell);
			}

			// perform a clustering of the genes participating in each cell type
			umapClustering(cellTypeClassifications, correlationThreshold, generatePDFCharts);

			// with no filtering

			// export to output: Prints cell type classifications into a table in a file
			printCellTypeClassifications(cellTypeClassifications, singleCellList, correlationThreshold,
					minNumberExpressedGenesInCell);

			// plots about suprema
			createScatterPlotOfSuprema(cellTypeClassifications, plotNegativeEnrichedCellTypes, generatePDFCharts);
			createHistogramOfSuprema(cellTypeClassifications, plotNegativeEnrichedCellTypes, generatePDFCharts);
			// print mapping of cell types
//				SingleCell.printCellTypeMapping(getCellTypesMappingOutputFile());
			// export file with genes involved in the correlations per cell type
			printGenesInvolvedInCorrelations(cellTypeClassifications, correlationThreshold);

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
					// export charts to a single PDF
					final File resultsSubfolder = getResultsSubfolderForCellTypes();
					try {
						if (generatePDFCharts) {
							logStatus("Writting charts into PDF files...");
							ChartsGenerated.getInstance().saveChartsAsPDF();
							logStatus("PDFs created");
						}
						// add pdf file to tar file

						// create tar.gz with all output files
						writeGZipOutputFile(getCurrentTimeStampFolder(), zipOutputFile);

						// set finish time
						runLog.setFinished(getDateNow());
						// update log
						runLogsRepo.save(runLog);

						if (runLog.getInputParameters().getEmail() != null) {
							EmailUtil.sendEmailWithResults(result, fromEmail);
						}
						log.info("PCTSEA got some results in "
								+ DatesUtil.getDescriptiveTimeFromMillisecs(result.getRunLog().getRunningTime()));
						log.info("Results file created at: " + result.getResultsFile());
						if (result.getUrlToViewer() != null) {
							log.info("Also, results can be visualized at: " + result.getUrlToViewer());
						}
					} catch (final IOException e) {
						e.printStackTrace();
						logStatus("Error writing PDF file with charts: " + e.getMessage(), LogLevel.ERROR);
					}
				}

				logStatus("Finishing now.");
			}
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
		inputParameters.setCellTypesClassification(cellTypeBranch);
		inputParameters.setEmail(email);
		inputParameters.setGeneratePDFCharts(generatePDFCharts);
		inputParameters.setInputDataFile(FilenameUtils.getName(experimentExpressionFile.getAbsolutePath()));
		inputParameters.setLoadRandom(loadRandomDistributionsIfExist);
		inputParameters.setMinCellsPerCellType(minCellsPerCellTypeForPDF);
		inputParameters.setMinCorrelation(correlationThreshold.getThresholdValue());
		inputParameters.setMinGenesCells(Double.valueOf(minNumberExpressedGenesInCell).intValue());
		inputParameters.setDataset(dataset);
		inputParameters.setNumPermutations(maxIterations);
		inputParameters.setOutputPrefix(prefix);
		inputParameters.setPlotNegativeEnriched(plotNegativeEnrichedCellTypes);
		return inputParameters;
	}

	public static final FastDateFormat dateFormatter = FastDateFormat.getInstance("yyyy-MM-dd_HH-mm-ss");

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

	private void printGeneExpression(List<SingleCell> singleCellsPassingCorrelationThreshold, String geneName,
			CorrelationThreshold correlationThreshold) throws IOException {
		FileWriter fw = null;
		try {
			ConcurrentUtil.sleep(1L);
			fw = new FileWriter(getGeneExpressionOutputFile(geneName, correlationThreshold));
			fw.write("single_cell\tcell_type\tbiomaterial\tcorrelation\texpression\n");
			for (final SingleCell singleCell : singleCellsPassingCorrelationThreshold) {

				final int ace2CellID = SingleCellsMetaInformationReader.getSingleCellIDBySingleCellName(geneName);
				final float expressionValue = singleCell.getGeneExpressionValue(ace2CellID);
				fw.write(singleCell.getName() + "\t" + singleCell.getCellType(cellTypeBranch) + "\t"
						+ singleCell.getBiomaterial() + "\t" + singleCell.getCorrelation() + "\t" + expressionValue
						+ "\n");
			}
		} finally {
			if (fw != null) {
				fw.close();
			}
		}
	}

	private void writeGZipOutputFile(File folder, File zipFile) throws IOException {
		logStatus("Compacting output files in single zip file...");
		// move out the correlations file so that is not included in the zip file

		final String fileName = FilenameUtils.getName(getCorrelationsOutputFile().getAbsolutePath());
		// it may be or not
		final File[] correlationsFiles = folder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {

				if (name.equals(fileName)) {
					return true;
				}
				return false;
			}
		});
		File tempFile = null;
		if (correlationsFiles.length > 0) {
			tempFile = File.createTempFile("correlations", "txt");
			Files.move(correlationsFiles[0], tempFile);
		}
		ZipManager.addFileToZipFile(folder, zipFile, true);
		if (correlationsFiles.length > 0) {
			// move back
			Files.move(tempFile, correlationsFiles[0]);
		}
		logStatus("Compressed file created at: " + zipFile.getAbsolutePath() + " ("
				+ FileUtils.getDescriptiveSizeFromBytes(zipFile.length()) + ")");
	}

	private void printGenesInvolvedInCorrelations(List<CellTypeClassification> cellTypeClassifications,
			CorrelationThreshold correlationThreshold) throws IOException {
		final File outputFile = getGenesInvolvedInCorrelationsOutputFile();
		FileWriter fw = null;
		try {
			ConcurrentUtil.sleep(1L);
			fw = new FileWriter(outputFile);
			// header
			fw.write("cell_type\tgene\tocurrence\n");

			// table
			for (final CellTypeClassification cellType : cellTypeClassifications) {
				final List<GeneOccurrence> geneOccurrences = cellType
						.getRankingOfGenesThatContributedToTheCorrelation(correlationThreshold);
				for (final GeneOccurrence geneOccurrence : geneOccurrences) {

					fw.write(cellType.getName() + "\t" + geneOccurrence.getGene() + "\t"
							+ geneOccurrence.getOccurrence() + "\n");
				}
			}
			logStatus("File with genes correlating in each cell type wrote at: " + outputFile.getAbsolutePath());
		} finally {
			if (fw != null) {
				fw.close();
			}
		}
	}

	private void umapClustering(List<CellTypeClassification> cellTypeClassifications,
			CorrelationThreshold correlationThreshold, boolean generateCharts) {
		final List<CellTypeClassification> cellTypesWithPositiveEnrichmentWeigthedScore = cellTypeClassifications
				.stream().filter(ct -> ct.getEnrichmentScore() > 0.0f).collect(Collectors.toList());
		logStatus(cellTypesWithPositiveEnrichmentWeigthedScore.size()
				+ " cell types with positive enrichment weigthed score");
		logStatus("Performing UMAP clusterings with different thresholds...");
		umapClustering(cellTypesWithPositiveEnrichmentWeigthedScore, correlationThreshold,
				"UMAP clustering of all cell types (no sig threshold)", true, generateCharts, "all");

		// by significancy < 0.01
		List<CellTypeClassification> significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> ct.getEnrichmentSignificance() < 0.01).collect(Collectors.toList());
		umapClustering(significantCellTypes, correlationThreshold,
				"UMAP clustering of significant cell types (sig<0.01)", false, generateCharts, "sig_0.01");
		// by significancy < 0.05
		significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> ct.getEnrichmentSignificance() < 0.05).collect(Collectors.toList());
		umapClustering(significantCellTypes, correlationThreshold,
				"UMAP clustering of significant cell types (sig<0.05)", false, generateCharts, "sig_0.05");
		// by hypergeometric p-value < 0.05
		significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> ct.getHypergeometricPValue() < 0.05).collect(Collectors.toList());
		umapClustering(significantCellTypes, correlationThreshold,
				"UMAP clustering of significant cell types by hypergeometric test (p-value<0.05)", false,
				generateCharts, "hypG_pvalue_0.05");
		// by Kolmogorov-Smirnov test: take only the ones with *,** or ***
		significantCellTypes = cellTypesWithPositiveEnrichmentWeigthedScore.stream()
				.filter(ct -> !"".equals(ct.getSignificancyString())).collect(Collectors.toList());
		umapClustering(significantCellTypes, correlationThreshold,
				"UMAP clustering of significant cell types by KS test (*, ** or ***)", false, generateCharts,
				"sig_KStest");

	}

	/**
	 * Creates an histogram plot with the values of the suprema for all cell types
	 * that has a positive enrichment score.
	 * 
	 * @param cellTypeClassifications
	 * @param plotNegativeEnrichedCellTypes
	 * @param generatePDFCharts
	 */
	private void createHistogramOfSuprema(List<CellTypeClassification> cellTypeClassifications,
			boolean plotNegativeEnrichedCellTypes, boolean generatePDFCharts) {
		final List<CellTypeClassification> cellTypes = cellTypeClassifications.stream()
				.filter(ct -> plotNegativeEnrichedCellTypes || ct.getEnrichmentScore() > 0.0f)
				.collect(Collectors.toList());

		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		final TDoubleList supremas = new TDoubleArrayList();
		for (final CellTypeClassification cellType : cellTypes) {
			final int supremumX = cellType.getSupremumX();
			if (supremumX != -1) {
				supremas.add(supremumX);
			}
		}

		String title = "Distribution of positive suprema positions in ranked cell list";

		final int numBins = Histogram.getRiceRuleForHistogramBins(supremas.size());
		final double[][] calcHistogram = smile.math.Histogram.histogram(supremas.toArray(), numBins);
		for (int i = 0; i < numBins; i++) {
			final double freq = calcHistogram[2][i];
			final double lowerBound = calcHistogram[0][i];
			final double upperBound = calcHistogram[1][i];
			dataset.addValue(freq, "x (rank of cells)", String.valueOf(Double.valueOf(lowerBound).intValue()) + "-"
					+ String.valueOf(Double.valueOf(upperBound).intValue()));
		}

		title += " (" + cellTypes.size() + ")";
		final JFreeChart chart = ChartFactory.createBarChart(title, "x (rank of cells)", "num cell types", dataset,
				PlotOrientation.VERTICAL, true, false, false);
		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setDefaultItemLabelGenerator(new IntegerCategoryItemLabelGenerator());
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
		renderer.setItemMargin(0.1);

		final CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		try {
			final String fileName = "suprema_hist";
			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			if (generatePDFCharts) {
				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
			}
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
	 * @param generatePDFCharts
	 */
	private void createScatterPlotOfSuprema(List<CellTypeClassification> cellTypeClassifications,
			boolean plotNegativeEnrichedCellTypes, boolean generatePDFCharts) {
		final List<CellTypeClassification> cellTypes = cellTypeClassifications.stream()
				.filter(ct -> plotNegativeEnrichedCellTypes || ct.getEnrichmentScore() > 0.0f)
				.collect(Collectors.toList());

		final LabeledXYDataset dataset = new LabeledXYDataset();
		final TDoubleList supremaXs = new TDoubleArrayList();
		for (final CellTypeClassification cellType : cellTypes) {
			final int supremumX = cellType.getSupremumX();
			final double supremum = cellType.getEnrichmentScore();
			supremaXs.add(supremumX);

			final String label = cellType.getName();

			dataset.add(cellType.getName(), supremumX, supremum, label);

		}

		final boolean legend = false;
		final boolean tooltips = true;
		final boolean urls = false;
		final JFreeChart chart = ChartFactory.createScatterPlot("Suprema position in ranked cell list vs suprema size",
				"Suprema position in ranked cell list", "supremum size", dataset, PlotOrientation.VERTICAL, legend,
				tooltips, urls);
		final XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		final XYItemRenderer renderer = plot.getRenderer();
		renderer.setDefaultItemLabelGenerator(new LabelGenerator());
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
		chart.addSubtitle(new TextTitle("(" + cellTypes.size() + " cell types)"));

		try {
			final String fileName = "suprema_scatter";
			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			if (generatePDFCharts) {
				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
			}
			logStatus("Scatter plot of suprema created.");
		} catch (final IOException e) {
			e.printStackTrace();
			logStatus("Some error occurred while creating chart for UMAP clustering: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	/**
	 * This creates a chart with an histogram of number of genes used for
	 * correlation for all single cells
	 * 
	 * @param singleCellList
	 */
	private void createDistributionOfCorrelationsOverRankedCells(List<SingleCell> singleCellList,
			CorrelationThreshold correlationThreshold) {

		PCTSEAUtils.sortByDescendingCorrelation(singleCellList);
		final XYSeriesCollection dataset = new XYSeriesCollection();
		final XYSeries positiveCorrelations = new XYSeries("corr >= " + correlationThreshold.getThresholdValue());
		dataset.addSeries(positiveCorrelations);
		final XYSeries negativeCorrelations = new XYSeries("corr < " + correlationThreshold.getThresholdValue());
		dataset.addSeries(negativeCorrelations);
		int numCell = 1;
		ConcurrentUtil.sleep(1L);
		for (final SingleCell singleCell : singleCellList) {
			if (!Double.isNaN(singleCell.getCorrelation())) {
				if (correlationThreshold.passThreshold(singleCell)) {
					positiveCorrelations.add(numCell, singleCell.getCorrelation());
				} else {
					negativeCorrelations.add(numCell, singleCell.getCorrelation());
				}

				numCell++;
			}
		}
		final JFreeChart chart = ChartFactory.createXYLineChart("Rank of cells by Pearson's correlation", "cell #",
				"Pearson's correlation", dataset);
		final XYPlot plot = (XYPlot) chart.getPlot();
		plot.getDomainAxis().setLowerBound(1.0);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setSeriesRenderingOrder(SeriesRenderingOrder.REVERSE);
		final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setSeriesPaint(2, Color.black);

		try {
			final String fileName = "corr_rank_dist";
			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			if (generatePDFCharts) {
				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
			}
			PCTSEA.logStatus("Rank of cells by Pearson's correlation plot of suprema created.");
		} catch (final IOException e) {
			e.printStackTrace();
			PCTSEA.logStatus("Some error occurred while creating chart for UMAP clustering: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	/**
	 * This creates a chart with an histogram of number of genes used for
	 * correlation for all single cells
	 * 
	 * @param singleCellList
	 */
	private void createHistogramOfCorrelatingGenes(List<SingleCell> singleCellList, Double minCorrelation) {

		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		final TIntIntMap map = new TIntIntHashMap();
		int totalCells = 0;
		ConcurrentUtil.sleep(1L);
		for (final SingleCell singleCell : singleCellList) {

			if (minCorrelation != null) {
				if (minCorrelation > singleCell.getCorrelation()) {
					continue;
				}
			}
			totalCells++;
			final int num = singleCell.getGenesForCorrelation().size();
			if (!map.containsKey(num)) {
				map.put(num, 1);
			} else {
				map.put(num, 1 + map.get(num));
			}
		}
		final TIntList keys = new TIntArrayList(map.keys());
		keys.sort();
		for (final int numGenes : keys.toArray()) {
			final int frequency = map.get(numGenes);
			dataset.addValue(frequency, "# genes", String.valueOf(numGenes));
		}
		for (int i = 0; i < keys.size(); i++) {
			int accumulativeNumGenes = 0;
			for (int j = i; j < keys.size(); j++) {
				accumulativeNumGenes += map.get(keys.get(j));

			}
			dataset.addValue(accumulativeNumGenes, "# genes or more", String.valueOf(keys.get(i)));
		}

		String title = "Distribution of # of genes correlating";
		if (minCorrelation != null) {
			title += " with only cells with corr >=" + minCorrelation;
		}
		title += " (" + totalCells + ")";
		final JFreeChart chart = ChartFactory.createBarChart(title, "# of genes correlating", "# cells", dataset,
				PlotOrientation.VERTICAL, true, false, false);
		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setDefaultItemLabelGenerator(new IntegerCategoryItemLabelGenerator());
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
		renderer.setItemMargin(0.1);
		renderer.setBarPainter(new StandardBarPainter());
		try {
			final String fileName = "genes_hist";
			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			if (generatePDFCharts) {
				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
			}
			// logStatus("Chart with the distribution of # of genes correlating is
			// created.");

		} catch (final IOException e) {
			e.printStackTrace();
			PCTSEA.logStatus(
					"Some error occurred while creating chart for histogram of correlating genes: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	/**
	 * 
	 * @param cellTypeClassifications
	 * @param correlationThreshold2
	 * @param chartTitle
	 * @param setAsDefaultUMAPOnCellType if true, the umap coordinates will be the
	 *                                   ones reported in the table
	 * @param generatePDFCharts
	 */
	private void umapClustering(List<CellTypeClassification> cellTypeClassifications,
			CorrelationThreshold correlationThreshold2, String chartTitle, boolean setAsDefaultUMAPOnCellType,
			boolean generatePDFCharts, String labelPDFFile) {
		if (cellTypeClassifications.isEmpty()) {
			return;
		}
		ConcurrentUtil.sleep(1L);
		final Set<String> totalGeneSet = new THashSet<String>();
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			final List<GeneOccurrence> geneOccurrences = cellType
					.getRankingOfGenesThatContributedToTheCorrelation(correlationThreshold2);
			geneOccurrences.forEach(go -> totalGeneSet.add(go.getGene()));
		}
		final List<String> totalGeneList = totalGeneSet.stream().collect(Collectors.toList());

		final float[][] instances = new float[cellTypeClassifications.size()][totalGeneSet.size()];
		for (int i = 0; i < cellTypeClassifications.size(); i++) {
			final CellTypeClassification cellType = cellTypeClassifications.get(i);
			final List<GeneOccurrence> geneOccurrences = cellType
					.getRankingOfGenesThatContributedToTheCorrelation(correlationThreshold2);
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
		umap.setNumberComponents(2);
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
				cellType.setUmapClustering(instance[0], instance[1]);
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
			final String fileName = "umap_" + labelPDFFile + "_scatter";
			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			if (generatePDFCharts) {
				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
			}
			// logStatus("UMAP clustering chart created");

		} catch (final IOException e) {
			e.printStackTrace();
			logStatus("Some error occurred while creating chart for UMAP clustering: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	private List<SingleCell> getSingleCellListFromDB(Dataset dataset) {

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
				ConcurrentUtil.sleep(1L);
			}

			cellID++;
			final SingleCell sc = new SingleCell(cellID, singleCelldb.getName(), Double.NaN);
			sc.setCellType(singleCelldb.getType());
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
	 * @param minCellsPerCellTypeForPDF
	 * @param plotNegativeEnrichedCellTypes
	 * @throws IOException
	 */
	private void calculateSignificanceByCellTypesPermutations(InteractorsExpressionsRetriever interactorExpressions,
			List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellListPassingCorrelationThreshold, CellTypeBranch cellTypeBranch,
			int maxIterations, boolean loadRandomDistributionsIfExist, int minCellsPerCellTypeForPDF,
			boolean plotNegativeEnrichedCellTypes) throws IOException {

		if (loadRandomDistributionsIfExist && getRandomScoresFile().exists() && getRandomScoresFile().length() > 0l) {
			readRandomDistributionFile(getRandomScoresFile(), cellTypeClassifications);
		} else {

			final List<String> originalCellTypes = new ArrayList<String>();
			singleCellListPassingCorrelationThreshold.forEach(c -> originalCellTypes.add(c.getCellType(null)));

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
					for (int i = 0; i < singleCellListPassingCorrelationThreshold.size(); i++) {
						final String permutatedCellType = permutatedCellTypes.get(i);
						singleCellListPassingCorrelationThreshold.get(i).setCellType(permutatedCellType);
					}

					// calculate enrichment scores with the Kolmogorov-Smirnov test
					calculateEnrichmentScore(cellTypeClassifications, singleCellListPassingCorrelationThreshold,
							cellTypeBranch, false, false, outputToLog, true, false, minCellsPerCellTypeForPDF,
							plotNegativeEnrichedCellTypes);

					counter.increment();
					final String printIfNecessary = counter.printIfNecessary();
					if (!"".equals(printIfNecessary)) {
						PCTSEA.logStatus(printIfNecessary);
					}
				} finally {
					iteration++;
				}

			}

			printToRandomDistributionFile(getRandomScoresFile(), cellTypeClassifications);
			PCTSEA.logStatus("Iterations  finished. Random scores distributions are stored at: "
					+ getRandomScoresFile().getAbsolutePath());
			// we set back the original correlations values
			for (int i = 0; i < singleCellListPassingCorrelationThreshold.size(); i++) {
				final String originalCellType = originalCellTypes.get(i);
				singleCellListPassingCorrelationThreshold.get(i).setCellType(originalCellType);
			}
		}

//		logStatus("Normalizing enrichment scores by cell type sizes...");
		// now we have all the random distributions. we can normalize by size.
		// we do that by dividing the real scores by the average of the random scores
		// this is done now when calling to cellType.getNormalizedEnrichmentScore()

		PCTSEA.logStatus("Calculating enrichment scores significance...");
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			final float realScore = cellType.getEnrichmentScore();
			if (Float.isNaN(realScore)) {
				cellType.setEnrichmentSignificance(Double.NaN);
				continue;
			}
			boolean negative = false;
			if (realScore < 0.0f) {
				negative = true;
			}
			final TFloatList randomEnrichmentScores = cellType.getRandomEnrichmentScores();
			final TDoubleList randomEnrichmentScores2 = new TDoubleArrayList();
			for (final float score : randomEnrichmentScores.toArray()) {
				if (!Float.isNaN(score)) {
					randomEnrichmentScores2.add(score);
				}
			}
//			final double pvalue = calculateProportionAll(realScore, randomEnrichmentScores2);
			double pvalue = calculateProportionPositive(realScore, randomEnrichmentScores2);
			if (negative) {
				pvalue = calculateProportionNegative(realScore, randomEnrichmentScores2);
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
		createFDRCalculationPlot(totalRealNormalizedScores, totalRandomNormalizedScores);
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

			final int sobs = nobs - (index >= 0 ? index : -index);
			final int snull = nnull - (index2 >= 0 ? index2 : -index2 - 1);

			final double fdr = (1d * snull / sobs) * (1d * nobs / nnull);
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
			TFloatList totalRandomNormalizedScores) {
		PCTSEA.logStatus("Creating FDR calculation plot with " + totalRealNormalizedScores.size() + " real scores and "
				+ totalRandomNormalizedScores.size() + " random scores");
		// create chart
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		final DecimalFormat format = new DecimalFormat("#.#");

		final int numBins = Math.max(Histogram.getSturgisRuleForHistogramBins(
				Math.max(totalRealNormalizedScores.size(), totalRandomNormalizedScores.size())), 10);

		final float max = Math.max(totalRealNormalizedScores.max(), totalRandomNormalizedScores.max());
		final float min = Math.min(totalRealNormalizedScores.min(), totalRandomNormalizedScores.min());
		double[][] binStats = Histogram.calcHistogram(totalRealNormalizedScores.toArray(), min, max, numBins);
		for (int i = 0; i < binStats[0].length; i++) {
			final double bin = binStats[2][i] / totalRealNormalizedScores.size();

			final double lowerBound = binStats[0][i];
			final double upperBound = binStats[1][i];
			final String columnKey = format.format(lowerBound) + " / " + format.format(upperBound);
			dataset.addValue(bin, "Observed", columnKey);
		}
		// TODO
		// no others as requested by Casimir
//				histogramDataset.addSeries(seriesOhersType);
		binStats = Histogram.calcHistogram(totalRandomNormalizedScores.toArray(), min, max, numBins);
		for (int i = 0; i < binStats.length; i++) {
			final double bin = binStats[2][i] / totalRandomNormalizedScores.size();
			final double lowerBound = binStats[0][i];
			final double upperBound = binStats[1][i];
			final String columnKey = format.format(lowerBound) + " / " + format.format(upperBound);
			dataset.addValue(bin, "Null", columnKey);
		}
		final String plotTitle = "Multiple testing correction";
		final String xaxis = "Normalized Enrichment Scores";
		final String yaxis = "Normalized Frequency";
		final PlotOrientation orientation = PlotOrientation.VERTICAL;
		final boolean show = true;
		final boolean toolTips = false;
		final boolean urls = false;

		final JFreeChart chart = ChartFactory.createBarChart(plotTitle, xaxis, yaxis, dataset, orientation, show,
				toolTips, urls);
		final CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setDefaultItemLabelGenerator(new DoubleCategoryItemLabelGenerator("#.##"));
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelFont(renderer.getDefaultItemLabelFont().deriveFont(8f));
		renderer.setItemMargin(0.1);
		renderer.setBarPainter(new StandardBarPainter());
		final CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

//		final ChartPanel panel = new ChartPanel(chart);
//		final JFrame frame = new JFrame();
//		frame.add(panel);
//		frame.setVisible(true);
		try {
			final String fileName = "ews_obs_null_hist";
			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			if (generatePDFCharts) {
				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, 500, 500, true, chartFile);
				PCTSEA.logStatus("Chart with the multiple testing correction is created.");
			}

		} catch (final IOException e) {
			e.printStackTrace();
			PCTSEA.logStatus("Some error occurred while creating chart for the multiple hypothesis correction: "
					+ e.getMessage(), LogLevel.ERROR);
		}

		return chart;
	}

	/**
	 * 
	 * @param interactorExpressions
	 * @param cellTypeClassifications
	 * @param singleCellList
	 * @param cellTypeBranch
	 * @param correlationThreshold
	 * @param loadRandomDistributionsIfExist
	 * @param generatePDFCharts
	 * @param minCellsPerCellTypeForPDF
	 * @param plotNegativeEnrichedCellTypes
	 * @param maxIterations
	 * @param takeZerosForCorrelation
	 * @param minNumberExpressedGenesInCell
	 * @throws IOException
	 */
	private void calculateSignificanceByPhenotypePermutations(InteractorsExpressionsRetriever interactorExpressions,
			List<CellTypeClassification> cellTypeClassifications, List<SingleCell> singleCellList,
			CellTypeBranch cellTypeBranch, CorrelationThreshold correlationThreshold,
			boolean loadRandomDistributionsIfExist, boolean generatePDFCharts, int minCellsPerCellTypeForPDF,
			boolean plotNegativeEnrichedCellTypes, int maxIterations, boolean takeZerosForCorrelation,
			int minNumberExpressedGenesInCell) throws IOException {

		// as well as the correlations
		final TIntDoubleMap correlationsBySingleCellID = new TIntDoubleHashMap();
		for (final SingleCell singleCell : singleCellList) {
			correlationsBySingleCellID.put(singleCell.getId(), singleCell.getCorrelation());
		}

		if (loadRandomDistributionsIfExist && getRandomScoresFile().exists() && getRandomScoresFile().length() > 0l) {
			readRandomDistributionFile(getRandomScoresFile(), cellTypeClassifications);
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
					correlateSingleCellsToInteractors(singleCellList, interactorExpressions, correlationThreshold,
							cellTypeBranch, false, outputToLog, false, takeZerosForCorrelation,
							minNumberExpressedGenesInCell);
					final List<SingleCell> singleCellsPassingCorrelationThreshold = correlationThreshold
							.getSingleCellsPassingThresholdSortedByCorrelation(singleCellList);
					// calculate enrichment scores with the Kolmogorov-Smirnov test
					calculateEnrichmentScore(cellTypeClassifications, singleCellsPassingCorrelationThreshold,
							cellTypeBranch, false, false, outputToLog, true, generatePDFCharts,
							minCellsPerCellTypeForPDF, plotNegativeEnrichedCellTypes);

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
					+ getRandomScoresFile().getAbsolutePath());
			printToRandomDistributionFile(getRandomScoresFile(), cellTypeClassifications);
		}
		// we set back the original correlations values
		for (final SingleCell singleCell : singleCellList) {
			singleCell.setCorrelation(correlationsBySingleCellID.get(singleCell.getId()));
		}
		// now we have all the distributions
		logStatus("Calculating enrichment scores significancy...");
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			final float realScore = cellType.getEnrichmentScore();
			boolean negative = false;
			if (realScore < 0.0f) {
				negative = true;
			}
			final TFloatList scoreDistributions = cellType.getRandomEnrichmentScores();

			final TDoubleList doubleScoreDistributions = new TDoubleArrayList();
			for (final float score : scoreDistributions.toArray()) {
				if (!Float.isNaN(score)) {
					doubleScoreDistributions.add(score);
				}
			}

			double pvalue = calculateProportionPositive(realScore, doubleScoreDistributions);

			if (negative) {
				pvalue = calculateProportionNegative(realScore, doubleScoreDistributions);
			}

			if (Double.isNaN(pvalue)) {
				int factor = 10;
				while (Double.isNaN(pvalue)) {
					factor += 10;
					// make the bins of the distribution smaller, so there is less gaps that can
					// make the value to fall into that gap and make pvalue to be NaN

					final int binCount = doubleScoreDistributions.size() / factor;
					if (binCount < 1) {
						break;
					}
					final EmpiricalDistribution distribution = new EmpiricalDistribution(binCount);
					distribution.load(doubleScoreDistributions.toArray());
					pvalue = 1 - distribution.cumulativeProbability(realScore);
					if (!Double.isNaN(pvalue)) {
						logStatus(cellType.getName() + " with factor " + factor + " with bins "
								+ distribution.getBinCount());
					}
				}
			}
			cellType.setEnrichmentSignificance(pvalue);
		}
	}

	private double calculateProportionAll(float realScore, TDoubleList doubleScoreDistributions) {
		doubleScoreDistributions.sort();
		int numHigherScores = 0;
		int total = 0; // this total only counts the positive values
		for (final double score : doubleScoreDistributions.toArray()) {
			total++;
			if (score >= realScore) {
				numHigherScores++;
			}
		}
		final double ret = 1.0 * numHigherScores / total;
		return ret;
	}

	private double calculateProportionPositive(float realScore, TDoubleList doubleScoreDistributions) {
		doubleScoreDistributions.sort();
		int numHigherScores = 0;
		int total = 0; // this total only counts the positive values
		for (final double score : doubleScoreDistributions.toArray()) {
			if (score <= 0) {
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

	private double calculateProportionNegative(float realScore, TDoubleList doubleScoreDistributions) {
		doubleScoreDistributions.sort();
		int total = 0; // this total only counts the negative values
		int numLowerScores = 0;
		for (final double score : doubleScoreDistributions.toArray()) {

			if (score >= 0) {
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
		// header
		fw.write("cell type\treal score\tdiff\tavg random score\tstd random score\trandom scores\n");

		Collections.sort(cellTypes, new Comparator<CellTypeClassification>() {

			@Override
			public int compare(CellTypeClassification o1, CellTypeClassification o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (final CellTypeClassification cellType : cellTypes) {
			final float realScore = cellType.getEnrichmentScore();
			fw.write(cellType.getName() + "\t" + realScore + "\t");
			final TFloatList randomScores = cellType.getRandomEnrichmentScores();
			final TFloatList randomKSTestDStatistics = cellType.getRandomKSTestDStatistics();
			final float mean = Maths.mean(randomScores);
			final double stddev = Maths.stddev(randomScores);
			final double diff = realScore - mean;
			fw.write(diff + "\t" + mean + "\t" + stddev);
			for (int i = 0; i < randomScores.size(); i++) {
				final double score = randomScores.get(i);
				final double dStatistic = randomKSTestDStatistics.get(i);
				fw.write("\t" + score + "\t" + dStatistic);
			}
			fw.write("\n");
		}
		fw.close();
		PCTSEA.logStatus("File with null distributions of scores written at: " + randomScoresFile.getAbsolutePath());
	}

	private File getRandomScoresFile() {
		return new File(getCurrentTimeStampPath() + prefix + "_" + maxIterations + "_random_scores.txt");
	}

	/**
	 * folder_where_iput_file_is/time_stamp/
	 * 
	 * @return
	 */
	private String getCurrentTimeStampPath() {
		return getCurrentTimeStampFolder().getAbsolutePath() + File.separator;
	}

	/**
	 * folder_where_iput_file_is/time_stamp/
	 * 
	 * @return
	 */
	private File getCurrentTimeStampFolder() {
		final String currenttimeStampPath = experimentExpressionFile.getParent() + File.separator + currentTimeStamp
				+ File.separator;
		final File file = new File(currenttimeStampPath);
		file.mkdirs();
		return file;
	}

	/**
	 * Prints cell type classifications into a table in a file.
	 * 
	 * @param cellTypeClassifications
	 * @param singleCellList
	 * @param correlationThreshold
	 * @param minNumberExpressedGenesInCell
	 * @throws IOException
	 */
	private void printCellTypeClassifications(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, CorrelationThreshold correlationThreshold,
			int minNumberExpressedGenesInCell) throws IOException {
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
		final long numSingleCellsWithPositiveCorrelation = correlationThreshold
				.getCountSingleCellsPassingThreshold(singleCellList);
		// print to file
		final File cellTypesFile = getCellTypesOutputFile();
		FileWriter fw = null;

		try {
			fw = new FileWriter(cellTypesFile);

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

			fw.write(paramsHeader.toString());

			fw.write("Total number of single cells with at least " + minNumberExpressedGenesInCell
					+ " genes present in the input data:\t" + singleCellList.size() + "\n");

			////////////
			// glossary of some columns:
			final StringBuilder glossary = new StringBuilder("Glossary of columns:\n");
			glossary.append("hyperG_p-value column:\tp-value obtained from performing an hypergeometric test\n");
			glossary.append(
					"log2_ratio column:\tRatio of ratios between the ratio of # cells of type passing correlation threshold and # all cells of type, divided by the ratio between all # cells of type and # total cells (log2((cells of type core "
							+ correlationThreshold + "/cells core " + correlationThreshold
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
					"Umap_x and Umap_y columns:\tCoordinates of the cell type after performing a Uniform Manifold Approximation and Projection (UMAP) clustering of all cell types with positive ews\n");
			glossary.append("'KS' term in this glossary:\tKolmogorov-Smirnov goodness-of-fit test\n");
			fw.write(glossary.toString() + "\n\n\n");

			////////////////////
			// Header of table
			fw.write("cell_type" //
					+ "\tnum_cells_of_type" //
					+ "\tnum_total_cells" //
					+ "\tnum_cells_of_type_corr" //
					+ "\tnum_cells_corr" //
					+ "\thyperG_p-value"//
					+ "\tlog2_ratio"//
//				+ "\teus"//
//				+ "\teus p-value"//
					+ "\tews"//
					+ "\tnorm-ews"//
					+ "\tsupX"//
					+ "\tnorm-supX"//
					+ "\tempirical_p-value"//
					+ "\tFDR"//
					+ "\t2nd_ews"//
					+ "\t2nd_supX"//
					+ "\tsize_a_type"//
					+ "\tsize_b_others"//
					+ "\tDab"//
					+ "\tKS_p-value"//
					+ "\tKS_p-value_BH_corrected"//
					+ "\tKS_significance_level"//
					+ "\tUmap_x"//
					+ "\tUmap_y"//

					+ "\tgenes"//

					+ "\n");
			boolean positiveScore = true;
			for (final CellTypeClassification cellType : cellTypeClassifications) {

				if (positiveScore && cellType.getEnrichmentScore() < 0.0f) {
					fw.write("------------------------- Negative ews from here -------------------------\n");
					positiveScore = false;
				}
				fw.write(cellType.getName() + "\t" + cellType.getNumCellsOfType() + "\t" + numSingleCells + "\t"
						+ cellType.getNumCellsOfTypePassingCorrelationThreshold() + "\t"
						+ numSingleCellsWithPositiveCorrelation + "\t" + cellType.getHypergeometricPValue() + "\t"
						+ cellType.getCasimirsEnrichmentScore() + "\t"
//					+ cellType.getEnrichmentUnweightedScore() + "\t"
						+ cellType.getEnrichmentScore() + "\t"//
						+ cellType.getNormalizedEnrichmentScore() + "\t"//
						+ cellType.getSupremumX() + "\t"//
						+ cellType.getNormalizedSupremumX() + "\t"//
						+ cellType.getEnrichmentSignificance() + "\t"//
						+ cellType.getEnrichmentFDR() + "\t"//
						+ parseNullableNumber(cellType.getSecondaryEnrichmentScore()) + "\t"//
						+ parseNullableNumber(cellType.getSecondarySupremumX()) + "\t"//
						+ cellType.getSizeA() + "\t" //
						+ cellType.getSizeB() + "\t" //
						+ cellType.getKSTestDStatistic() + "\t"//
						+ cellType.getKSTestPvalue() + "\t"//
						+ cellType.getKSTestCorrectedPvalue() + "\t"//
						+ cellType.getSignificancyString() + "\t"//

						+ parseNullableNumber(cellType.getUmapClusteringX()) + "\t" //
						+ parseNullableNumber(cellType.getUmapClusteringY()) + "\t" //

						+ cellType.getStringOfRankingOfGenesThatContributedToTheCorrelation(correlationThreshold)
						+ "\n");
				fw.flush();
			}
			logStatus("File writen at " + cellTypesFile.getAbsolutePath());
		} finally {
			fw.close();
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
		sb.append(InputParameters.CHARTS + " = " + generatePDFCharts + "\n");
		sb.append(InputParameters.MIN_CORRELATION + " = " + correlationThreshold.getThresholdValue() + "\n");
		sb.append(InputParameters.MIN_GENES_CELLS + " = " + minNumberExpressedGenesInCell + "\n");
		sb.append(InputParameters.CELL_TYPES_CLASSIFICATION + " = " + cellTypeBranch + "\n");
		sb.append(InputParameters.LOAD_RANDOM + " = " + loadRandomDistributionsIfExist + "\n");
		sb.append(InputParameters.MIN_CELLS_PER_CELL_TYPE + " = " + minCellsPerCellTypeForPDF + "\n");
		sb.append(InputParameters.PLOT_NEGATIVE_ENRICHED + " = " + plotNegativeEnrichedCellTypes + "\n");
		return sb.toString();
	}

	private String parseNullableNumber(Number number) {
		if (number == null) {
			return "";
		}
		return number.toString();
	}

	private List<CellTypeClassification> calculateHyperGeometricStatistics(List<SingleCell> singleCellList,
			CellTypeBranch cellTypeBranch, CorrelationThreshold correlationThreshold) {
		final List<String> celltypes = singleCellList.stream().map(sc -> sc.getCellType(cellTypeBranch))
				.filter(ct -> ct != null).distinct().sorted().collect(Collectors.toList());

		final String message = celltypes.size() + " different cell types with branch " + cellTypeBranch;
		PCTSEA.logStatus(message);

		// by cell types

		final int numSingleCells = singleCellList.size();

		final List<SingleCell> singleCellsWithPositiveCorrelation = correlationThreshold
				.getSingleCellsPassingThreshold(singleCellList);
		final int numSingleCellsWithPositiveCorrelation = singleCellsWithPositiveCorrelation.size();
		PCTSEA.logStatus(numSingleCellsWithPositiveCorrelation + " single cells with correlation passing threshold "
				+ correlationThreshold);
		PCTSEA.logStatus("Calculating hypergeometric statistics...");
		//
		int N; // population size
		int K; // successes in entire population
		int n; // sample size, number of draws
		int k; // observed successes in sample
		//
		int numSignificantCellTypes = 0;
		N = numSingleCells;
		final List<CellTypeClassification> cellTypeClassifications = new ArrayList<CellTypeClassification>();
		for (final String cellType : celltypes) {
			ConcurrentUtil.sleep(1L);
			// cells of type
			final List<SingleCell> cellsOfCellType = singleCellList.stream().filter(
					sc -> sc.getCellType(cellTypeBranch) != null && sc.getCellType(cellTypeBranch).equals(cellType))
					.collect(Collectors.toList());
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

			final int numCellsOfTypeWithPositiveCorrelation = (int) correlationThreshold
					.getCountSingleCellsPassingThreshold(cellsOfCellType);

			K = numSingleCellsWithPositiveCorrelation;
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
			n = numSingleCellsWithPositiveCorrelation;
			k = numCellsOfTypeWithPositiveCorrelation;
			final HypergeometricDistribution hg2 = new HypergeometricDistribution(N, K, n);
			double p2 = hg2.upperCumulativeProbability(k);
			if (p2 < 0.0) {
				p2 = 0.0;
			}

			final CellTypeClassification cellTypeClassification = new CellTypeClassification(cellType, p);
			cellTypeClassification.setNumCellsOfType(numCellsOfType);
			cellTypeClassification.setNumCellsOfTypePassingCorrelationThreshold(numCellsOfTypeWithPositiveCorrelation);
			cellTypeClassification.setSingleCells(cellsOfCellType);

			// casimir's enrichment score
			// log2((cells of type core >0.1/cells core >0.1)/(cells of type/total cells))

			if (numCellsOfType > 0 && numSingleCellsWithPositiveCorrelation > 0) {
				final double x = (1.0 * numCellsOfTypeWithPositiveCorrelation / numSingleCellsWithPositiveCorrelation)
						/ (1.0 * numCellsOfType / numSingleCells);
				final Double casimirsEnrichmentScore = Maths.log(x, 2);
				cellTypeClassification.setCasimirsEnrichmentScore(casimirsEnrichmentScore.floatValue());
			} else {
				PCTSEA.logStatus(cellType + " cannot have a casimirs score because # of '" + cellType + "' cells is "
						+ numCellsOfType + " and # of '" + cellType + "' cells with corr " + correlationThreshold
						+ " is " + numSingleCellsWithPositiveCorrelation);
			}
			cellTypeClassifications.add(cellTypeClassification);
		}
		PCTSEA.logStatus("Hypergeometric statistics calculated. " + numSignificantCellTypes
				+ " cell types are significative (pvalue<0.05)");
		return cellTypeClassifications;
	}

	/**
	 * Calculate correlations between the expression of the experimental proteins
	 * and the single cells
	 * 
	 * @param singleCellList
	 * @param interactorExpressions
	 * @param correlationThreshold
	 * @param cellTypeBranch
	 * @param writeCorrelationsFile
	 * @param outputToLog
	 * @param getExpressionsUsedForCorrelation
	 * @param takeZerosForCorrelation
	 * @param minNumberExpressedGenesInCell
	 * @return the number of cells that pass the correlation threshold
	 * @throws IOException
	 */
	private int correlateSingleCellsToInteractors(List<SingleCell> singleCellList,
			InteractorsExpressionsRetriever interactorExpressions, CorrelationThreshold correlationThreshold,
			CellTypeBranch cellTypeBranch, boolean writeCorrelationsFile, boolean outputToLog,
			boolean getExpressionsUsedForCorrelation, boolean takeZerosForCorrelation,
			int minNumberExpressedGenesInCell) throws IOException {
		final File correlationsOutputFile = getCorrelationsOutputFile();
		final int originalNumCells = singleCellList.size();
		int numPositiveCorrelated = 0;
		if (outputToLog) {
			PCTSEA.logStatus("Calculating correlations...");
		}
		// output correlations
		FileWriter correlationsFileWriter = null;
		try {
			if (writeCorrelationsFile) {
				correlationsFileWriter = new FileWriter(correlationsOutputFile);
				correlationsFileWriter.write(
						"cell\tcell_type\tpearsons_corr\tvalues_correlated\tgenes\tnum_genes\tgene_expression_variance_on_cell\n");
			}
			final Iterator<SingleCell> cellsIterator = singleCellList.iterator();
			final ProgressCounter counter = new ProgressCounter(originalNumCells, ProgressPrintingType.PERCENTAGE_STEPS,
					0, true);
			counter.setSuffix("calculating correlations");
			while (cellsIterator.hasNext()) {
				counter.increment();
				final String printIfNecessary = counter.printIfNecessary();
				if (!"".equals(printIfNecessary)) {
					ConcurrentUtil.sleep(1L);
					logStatus(printIfNecessary);
				}
				final SingleCell singleCell = cellsIterator.next();

				singleCell.calculateCorrelation(interactorExpressions, takeZerosForCorrelation,
						minNumberExpressedGenesInCell, getExpressionsUsedForCorrelation);
//			if (Double.isNaN(singleCell.getGeneExpressionVariance())
//					|| singleCell.getGeneExpressionVariance() < minCellsPerCellTypeForPDF) {
//				numCellsDiscardedByMinimumVariance++;
//				cellsIterator.remove();
//				continue;
//			}
				if (correlationThreshold.passThreshold(singleCell)) {
					numPositiveCorrelated++;
				}
			}
			logStatus("Correlations between single cell expressions and input data are calculated.");

			ProgressCounter counter2 = null;
			if (writeCorrelationsFile) {
				logStatus("Writting correlations to file...");
				counter2 = new ProgressCounter(singleCellList.size(), ProgressPrintingType.PERCENTAGE_STEPS, 0, true);
			}

			// we sort the single cell list to have them sorted by correlation
			correlationThreshold.sortSingleCellsByCorrelation(singleCellList);

			// print to file and create chart
			final TDoubleList correlations = new TDoubleArrayList();
			ConcurrentUtil.sleep(1L);

			for (final SingleCell singleCell : singleCellList) {
				if (!Double.isNaN(singleCell.getCorrelation())) {
					correlations.add(singleCell.getCorrelation());
				}
				if (writeCorrelationsFile) {
					counter2.increment();
					final String printIfNecessary = counter2.printIfNecessary();
					if (!"".equals(printIfNecessary)) {
						logStatus(printIfNecessary);
					}
					correlationsFileWriter.write(singleCell.getName() + "\t" + singleCell.getCellType(cellTypeBranch)
							+ "\t" + +singleCell.getCorrelation() + "\t" + singleCell.getExpressionsUsedForCorrelation()
							+ "\t" + singleCell.getGenesForCorrelationString() + "\t"
							+ singleCell.getGenesForCorrelation().size() + "\t" + singleCell.getGeneExpressionVariance()
							+ "\n");
				}
			}

			createWholeDatasetCorrelationDistributionChart(correlations);
			if (writeCorrelationsFile) {
				logStatus("Correlations written to file single cell expressions and input data are done.");
			}

			return numPositiveCorrelated;
		} finally {
			if (writeCorrelationsFile) {
				correlationsFileWriter.close();
			}
			if (outputToLog) {
				logStatus(numPositiveCorrelated + " cells pass the correlation threshold out of " + originalNumCells);

				if (writeCorrelationsFile) {
					logStatus("Correlations file created at: " + correlationsOutputFile.getAbsolutePath());
				}
			}
		}

	}

	private void createWholeDatasetCorrelationDistributionChart(TDoubleList correlations) {

		// create chart
		final HistogramDataset histogramDataset = new HistogramDataset();

		histogramDataset.addSeries(correlations.size() + " single cells", correlations.toArray(), 50);
		final String plotTitle = "Pearson's correlation distribution";
		final String xaxis = "Pearson's correlation";
		final String yaxis = "Frequency (# of cells)";
		final PlotOrientation orientation = PlotOrientation.VERTICAL;
		final boolean show = true;
		final boolean toolTips = false;
		final boolean urls = false;
		final JFreeChart chart = ChartFactory.createXYLineChart(plotTitle, xaxis, yaxis, histogramDataset, orientation,
				show, toolTips, urls);

		final File folder = getResultsSubfolderForCellTypes(prefix);
		if (!folder.exists()) {
			folder.mkdirs();
			logStatus("Folder '" + folder.getAbsolutePath() + "' created");
		}
		final int width = 500;
		final int height = 500;
		try {
			final String fileName = "corr_hist";
			PCTSEAUtils.writeTXTFileForChart(chart, getResultsSubfolderGeneral(), prefix, fileName);
			if (generatePDFCharts) {
				final File chartFile = PCTSEAUtils.getChartPDFFile(getResultsSubfolderGeneral(), fileName, prefix);
				ChartsGenerated.getInstance().saveScaledChartAsPNGInMemory(chart, width, height, true, chartFile);
			}
		} catch (final IOException e) {
			PCTSEA.logStatus("Some error occurred while creating chart for correlations: " + e.getMessage(),
					LogLevel.ERROR);
		}

	}

	/**
	 * 
	 * @param cellTypeClassifications
	 * @param singleCellList                 cells filtered already by correlation
	 *                                       threshold
	 * @param cellTypeBranch
	 * @param calculateUnweighted
	 * @param calculateKolmogorovSmirnovTest
	 * @param outputToLog
	 * @param permutatedData
	 * @param generatePDFCharts
	 * @param minCellsPerCellTypeForPDF
	 * @param plotNegativeEnrichedCellTypes
	 */
	private void calculateEnrichmentScore(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, CellTypeBranch cellTypeBranch, boolean calculateUnweighted,
			boolean calculateKolmogorovSmirnovTest, boolean outputToLog, boolean permutatedData,
			boolean generatePDFCharts, int minCellsPerCellTypeForPDF, boolean plotNegativeEnrichedCellTypes) {
		if (outputToLog) {
			PCTSEA.logStatus("Calculating enrichment scores...");
		}
		// first set NaN correlations to zero
		singleCellList.stream().forEach(s -> {
			if (Double.isNaN(s.getCorrelation())) {
				s.setCorrelation(0.0);
			}
		});

		// calculate unweighted Score
		if (calculateUnweighted) {
		}
		calculateUnweigthedScore(cellTypeClassifications, singleCellList, cellTypeBranch, outputToLog);

		// calculate weighted Score
		calculateWeigthedScoreInParallel(cellTypeClassifications, singleCellList, cellTypeBranch, outputToLog,
				permutatedData, generatePDFCharts, minCellsPerCellTypeForPDF, plotNegativeEnrichedCellTypes);

		// sort by score descending
		Collections.sort(cellTypeClassifications, new Comparator<CellTypeClassification>() {

			@Override
			public int compare(CellTypeClassification o1, CellTypeClassification o2) {
				return Double.compare(o2.getEnrichmentScore(), o1.getEnrichmentScore());
			}
		});

		if (calculateKolmogorovSmirnovTest) {
			// calculate significance
			calculateKolmogorovSmirnovTestInParallel(cellTypeClassifications, outputToLog);
		}
	}

	private void calculateKolmogorovSmirnovTestInParallel(List<CellTypeClassification> cellTypeClassifications,
			boolean outputToLog) {
//		final KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
		if (outputToLog) {
			PCTSEA.logStatus(
					"Calculating enrichment significancy with KS test (in parallel with " + threadCount + " cores)...");
		}
		final ParIterator<CellTypeClassification> iterator = ParIteratorFactory
				.createParIterator(cellTypeClassifications, threadCount, Schedule.GUIDED);
//		final Reducible<List<CellTypeClassification>> reducibles = new Reducible<List<CellTypeClassification>>();
		final List<KolmogorovSmirnovTestParallel> runners = new ArrayList<KolmogorovSmirnovTestParallel>();
		for (int numCore = 1; numCore <= threadCount; numCore++) {
			// take current DB session
			final KolmogorovSmirnovTestParallel runner = new KolmogorovSmirnovTestParallel(iterator, numCore);
			runners.add(runner);
			runner.start();
		}

		// Main thread waits for worker threads to complete
		for (int k = 0; k < threadCount; k++) {
			try {
				runners.get(k).join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void calculateWeigthedScoreInParallel(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, CellTypeBranch cellTypeBranch, boolean outputToLog, boolean permutatedData,
			boolean generatePDFCharts, int minCellsPerCellTypeForPDF, boolean plotNegativeEnrichedCellTypes) {
		if (outputToLog) {
			PCTSEA.logStatus("Calculating weigthed enrichment score and KS statistics...");
		}
		final ParIterator<CellTypeClassification> iterator = ParIteratorFactory
				.createParIterator(cellTypeClassifications, threadCount, Schedule.GUIDED);
		final List<EnrichmentWeigthedScoreParallel> runners = new ArrayList<EnrichmentWeigthedScoreParallel>();
		for (int numCore = 1; numCore <= threadCount; numCore++) {
			// take current DB session
			final EnrichmentWeigthedScoreParallel runner = new EnrichmentWeigthedScoreParallel(iterator, numCore,
					singleCellList, cellTypeBranch, permutatedData, minCellsPerCellTypeForPDF,
					plotNegativeEnrichedCellTypes);
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
			saveScoreCalculationCharts(cellTypeClassifications, generatePDFCharts);
		}
	}

	private void saveScoreCalculationCharts(List<CellTypeClassification> cellTypeClassifications,
			boolean generatePDFCharts) {

		final List<CellTypeClassification> newList = new ArrayList<CellTypeClassification>();
		newList.addAll(cellTypeClassifications);

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				PCTSEA.logStatus("Creating charts for each cell type in the background...");
				final File resultsSubfolderForCellTypes = getResultsSubfolderForCellTypes();

				// we create the writableImages on the FX thread and store them in a list, so
				// that then we can save them
				for (final CellTypeClassification cellTypeClassification : newList) {
					try {
						cellTypeClassification.saveCharts(resultsSubfolderForCellTypes, prefix, generatePDFCharts);
					} catch (final IOException e) {
						e.printStackTrace();
						PCTSEA.logStatus("Some error occurred while saving chart for "
								+ cellTypeClassification.getName() + ": " + e.getMessage(), LogLevel.ERROR);
					}
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
	 * @param cellTypeBranch
	 */
	private void calculateUnweigthedScore(List<CellTypeClassification> cellTypeClassifications,
			List<SingleCell> singleCellList, CellTypeBranch cellTypeBranch, boolean outputToLog) {
		if (outputToLog) {
			PCTSEA.logStatus("Calculating unweigthed enrichment score...");
		}
		final long n = singleCellList.size();
		for (final CellTypeClassification cellType : cellTypeClassifications) {
			double score = -Double.MAX_VALUE;
			final String cellTypeName = cellType.getName();
			final long nk = singleCellList.stream().filter(c -> cellTypeName.equals(c.getCellType(cellTypeBranch)))
					.count();
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

				if (cellTypeName.equals(singleCell.getCellType(cellTypeBranch))) {
					numeratorA++;
					a = 1.0 * numeratorA / denominatorA;
					distA.add(singleCell.getCorrelation());
					b = previousB;
				} else {
					numeratorB++;
					a = previousA;
					b = 1.0 * numeratorB / denominatorB;
					distB.add(singleCell.getCorrelation());
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

	private File getResultsSubfolderGeneral() {
		return getResultsSubfolderGeneral(prefix);
	}

	private File getResultsSubfolderForCellTypes() {
		return getResultsSubfolderForCellTypes(prefix);
	}

	private File getResultsSubfolderGeneral(String prefix) {
		final File folder = new File(getCurrentTimeStampPath() + "global_charts");
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	private File getResultsSubfolderForCellTypes(String prefix) {
		final File folder = new File(getCurrentTimeStampPath() + "cell_types_charts");
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	private File getZipOutputFile() {
		File file = new File(getCurrentTimeStampFolder().getParent() + File.separator
				+ FilenameUtils.getBaseName(getCurrentTimeStampFolder().getAbsolutePath()) + ".zip");
		if (file.exists()) {
			int i = 2;
			while (file.exists()) {
				file = new File(getCurrentTimeStampFolder().getParent() + File.separator
						+ FilenameUtils.getBaseName(getCurrentTimeStampFolder().getAbsolutePath()) + "_" + i + ".zip");
				i++;
			}
		}
		return file;
	}

	private File getGenesInvolvedInCorrelationsOutputFile() {
		return new File(getCurrentTimeStampPath() + prefix + "_correlation_genes.txt");
	}

	private File getCellTypesOutputFile() {
		return new File(getCurrentTimeStampPath() + prefix + "_cell_types_enrichment.txt");
	}

	private File getCorrelationsOutputFile() {
		return new File(getCurrentTimeStampPath() + prefix + "_single_cell_correlations.txt");
	}

	private File getGeneExpressionOutputFile(String geneName, CorrelationThreshold correlationThreshold) {
		return new File(getCurrentTimeStampPath() + prefix + "_" + geneName + "_expressions_with_corr_"
				+ correlationThreshold.getThresholdValue() + ".txt");
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

	public void setCorrelationThreshold(CorrelationThreshold correlationThreshold2) {
		correlationThreshold = correlationThreshold2;
	}

	public void setMinNumberExpressedGenesInCell(int minNumberExpressedGenesInCell2) {
		minNumberExpressedGenesInCell = minNumberExpressedGenesInCell2;
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

	public void setGenerateCharts(boolean generateCharts2) {
		generatePDFCharts = generateCharts2;
	}

	public void setMinCellsPerCellTypeForPDF(int minCellsPerCellTypeForPDF2) {
		minCellsPerCellTypeForPDF = minCellsPerCellTypeForPDF2;
	}

	public void setPlotNegativeEnrichedCellTypes(boolean plotNegativeEnrichedCellTypes2) {
		plotNegativeEnrichedCellTypes = plotNegativeEnrichedCellTypes2;
	}

	public void setStatusListener(StatusListener statusListener) {
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

	/**
	 * Log to {@link StatusListener} and to the app logging system
	 * 
	 * @param message
	 * @param level
	 */
	public static void logStatus(String message, LogLevel level) {
		if (statusListener != null) {
			statusListener.onStatusUpdate(
//					format.format(new Date()) + ": " + 
					message);
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
		return writeCorrelationsFile;
	}

	public void setWriteCorrelationsFile(boolean writeCorrelationsFile) {
		this.writeCorrelationsFile = writeCorrelationsFile;
	}
}
