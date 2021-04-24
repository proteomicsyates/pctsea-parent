package edu.scripps.yates.pctsea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import edu.scripps.yates.annotations.uniprot.UniprotProteinLocalRetriever;
import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLog;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.Gene;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import edu.scripps.yates.utilities.annotations.uniprot.UniprotEntryUtil;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.pi.ConcurrentUtil;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import edu.scripps.yates.utilities.strings.StringUtils;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

public class InteractorsExpressionsRetriever {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(InteractorsExpressionsRetriever.class);
	private final TIntObjectMap<Gene> genesById = new TIntObjectHashMap<Gene>();
	private final TIntFloatMap interactorExpressionsInOurExperiment = new TIntFloatHashMap();
	private final TIntList geneIDs = new TIntArrayList();
	private final List<String> genes;
	private static final TObjectIntMap<String> geneIDsByGeneNameMap = new TObjectIntHashMap<String>();
	private static final TIntObjectMap<String> geneNamesByGeneIDMap = new TIntObjectHashMap<String>();
	private final ExpressionMongoRepository expresssionsMongoRepository;
	private final MongoBaseService mongoBaseService;
	private final Dataset dataset;
	private final String uniprotRelease;
	private final PctseaRunLog runLog;
	private static InteractorsExpressionsRetriever instance;
	private final CellTypeBranch cellTypeBranch;

	/**
	 * 
	 * @param expresssionsMongoRepository
	 * @param experimentalExpressionsFile
	 * @param minNumInteractorsForCorrelation
	 * @param uniprotRelease
	 * @param runLog                          to log the number of genes found from
	 *                                        input list in the database
	 * @param cellTypeBranch
	 * @throws IOException
	 */
	public InteractorsExpressionsRetriever(ExpressionMongoRepository expresssionsMongoRepository,
			MongoBaseService mongoBaseService, File experimentalExpressionsFile, Dataset dataset, String uniprotRelease,
			PctseaRunLog runLog, CellTypeBranch cellTypeBranch) throws IOException {
//		if (SingleCellsMetaInformationReader.singleCellIDsBySingleCellNameMap.isEmpty()) {
//			throw new IllegalArgumentException(
//					"We need to read the single cell metainformation before reading expressions");
//		}
		// clear static
		geneIDsByGeneNameMap.clear();
		geneNamesByGeneIDMap.clear();
		this.runLog = runLog;
		this.dataset = dataset;
		this.mongoBaseService = mongoBaseService;
		this.expresssionsMongoRepository = expresssionsMongoRepository;
		this.uniprotRelease = uniprotRelease;
		this.cellTypeBranch = cellTypeBranch;
		genes = readExperimentalExpressionsFile(experimentalExpressionsFile);
		instance = this;
	}

	public static InteractorsExpressionsRetriever getInstance() {
		return instance;
	}

	/**
	 * 
	 * @param cellsMetadata
	 * @param singleCellExpressionsFile table with single cells as columns, and rows
	 *                                  as gene/protein names
	 * @param dataset
	 * @throws IOException
	 */
	private void getSingleCellExpressionsFromDB(List<String> inputProteinGeneList) {
		PCTSEA.logStatus("Getting single-cell expression profiles with the input protein/gene list...");
//		PCTSEA.logStatus(geneIDsByGeneNameMap.size() + " - " + geneNamesByGeneIDMap.size());
		final Set<String> singleCellNames = new THashSet<String>();
		final List<String> genesNotFound = new ArrayList<String>();
		final List<String> genesFound = new ArrayList<String>();

		final Map<String, List<String>> genesByInputEntry = getGenesFromInputList(inputProteinGeneList);

		final ProgressCounter counter = new ProgressCounter(inputProteinGeneList.size(),
				ProgressPrintingType.PERCENTAGE_STEPS, 0, true);
		counter.setSuffix("Getting expression profiles of interest...");
		for (final String inputProteinGene : inputProteinGeneList) {
			boolean inputProteinGeneFound = false;
			counter.increment();
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				PCTSEA.logStatus(printIfNecessary, false);
			}
			final List<String> genes = genesByInputEntry.get(inputProteinGene);
			for (final String geneName : genes) {

				final List<Expression> expressions = mongoBaseService.getExpressionByGene(geneName, dataset);

				// it already has an id associated from the call to getGenesFromInputList
				final int geneID = geneIDsByGeneNameMap.get(geneName);
				if (expressions == null || expressions.isEmpty()) {
					continue; // we try the next one
				} else {
					genesFound.add(geneName);
				}

				final Gene gene = new Gene(geneID, geneName);
				genesById.put(geneID, gene);
				geneIDs.add(geneID);
				for (final Expression expression : expressions) {

					final float interactorExpressionInSingleCell = expression.getExpression();
					final String singleCellName = expression.getCellName();
					final int singleCellID = SingleCellsMetaInformationReader
							.getSingleCellIDBySingleCellName(singleCellName);
					if (singleCellID == -1) {
						// this happens if the cell was ignored when retrieved from the DB because it
						// didn't have a type
						continue;
					}
					singleCellNames.add(singleCellName);
					final SingleCell cell = SingleCellsMetaInformationReader.getSingleCellByCellID(singleCellID);
					cell.addGeneExpressionValue(geneID, interactorExpressionInSingleCell);
					final String cellTypeName = cell.getCellType(this.cellTypeBranch);
					gene.addExpressionValueInSingleCell(singleCellID, interactorExpressionInSingleCell, cellTypeName);
				}
				if (!expressions.isEmpty()) {
					inputProteinGeneFound = true;
					// here we override any synonym that we could have associated with that geneID
					// number, so that we final associated with the final actual gene name final
					// that have the final expression values in final the DB
					geneNamesByGeneIDMap.put(geneID, geneName.toUpperCase());
					break;
				}
			}
			if (!inputProteinGeneFound) {
				genesNotFound.add(inputProteinGene);
			}
		}

//		System.out.println(message);

		// log num input genes that are not found
		runLog.setInputGenesNotFound(genesNotFound);
		if (!genesNotFound.isEmpty()) {
			final String message2 = "Expression values from " + genesFound.size()
					+ " input entries were found in the database. Expression values from " + genesNotFound.size()
					+ " genes were NOT found in the single cells dataset:";
			PCTSEA.logStatus(message2);
//			System.out.println(message2);
			String message3 = "";
			for (final String geneNotFound : genesNotFound) {
				message3 += geneNotFound + ",";
			}
			PCTSEA.logStatus(message3);
//			System.out.println(message3);
		}
		final String message = singleCellNames.size() + " single cells with expression values in " + genesById.size()
				+ " different genes/proteins out of a total " + SingleCellsMetaInformationReader.getNumSingleCells()
				+ " of single cells";
		PCTSEA.logStatus(message);

	}

	/**
	 * Returns a map for each input element, mapped to a list of genes
	 * 
	 * @param inputProteinGeneList
	 * @return
	 */
	private Map<String, List<String>> getGenesFromInputList(List<String> inputProteinGeneList) {
		final Map<String, List<String>> genesByInputEntry = new THashMap<String, List<String>>();
		// get annotations from uniprot so I can map to gene names and their synonyms
		final List<String> uniprotAccs = new ArrayList<String>();
		for (final String proteinGene : inputProteinGeneList) {
			if (FastaParser.isUniProtACC(proteinGene)) {
				uniprotAccs.add(proteinGene);
			} else {
				genesByInputEntry.put(proteinGene, new ArrayList<String>());
				genesByInputEntry.get(proteinGene).add(proteinGene.toUpperCase());
			}
		}
		final Map<String, Entry> annotatedProteins = new THashMap<String, Entry>();
		if (!uniprotAccs.isEmpty()) {
			PCTSEA.logStatus("Translating " + uniprotAccs.size() + " uniprot accessions to gene names");

			final UniprotProteinLocalRetriever uplr = new UniprotProteinLocalRetriever(
					new File(System.getProperty("user.dir")), true);
			uplr.setRetrieveFastaIsoforms(false);
			uplr.setRetrieveFastaIsoformsFromMainForms(false);
			annotatedProteins.putAll(uplr.getAnnotatedProteins(uniprotRelease, uniprotAccs));

			for (final String uniprotAcc : uniprotAccs) {
				genesByInputEntry.put(uniprotAcc, new ArrayList<String>());
				final int geneID = geneIDsByGeneNameMap.get(uniprotAcc);
				if (annotatedProteins.containsKey(uniprotAcc)) {
					final Entry entry = annotatedProteins.get(uniprotAcc);
					if (entry != null) {
						final List<String> geneNames = getGeneNames(UniprotEntryUtil.getGeneName(entry, false, true));
						for (final String geneName2 : geneNames) {
							genesByInputEntry.get(uniprotAcc).add(geneName2.toUpperCase());
							geneIDsByGeneNameMap.put(geneName2.toUpperCase(), geneID);
							// here we are overriding some gene names but it is ok, it is just to later
							// print which genes were involved on each correlation calculation
							geneNamesByGeneIDMap.put(geneID, geneName2.toUpperCase());
							geneIDsByGeneNameMap.remove(uniprotAcc);
						}
					}
				} else {
					genesByInputEntry.get(uniprotAcc).add(uniprotAcc);
				}
			}
		}
		return genesByInputEntry;
	}

	/**
	 * Returns a list of gene names making sure that the first in the list is the
	 * primary one.
	 * 
	 * @param geneNames
	 * @return
	 */
	private List<String> getGeneNames(List<Pair<String, String>> geneNames) {
		final List<String> ret = new ArrayList<String>();
		for (final Pair<String, String> pair : geneNames) {
			if (pair.getSecondElement().equals("primary")) {
				ret.add(0, pair.getFirstelement());
			} else {
				ret.add(pair.getFirstelement());
			}
		}
		return ret;
	}

	/**
	 * Reads the experimental expression data of a set of genes
	 * 
	 * @param experimentalExpressionsFile
	 * @return the list of genes
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	private List<String> readExperimentalExpressionsFile(File experimentalExpressionsFile)
			throws NumberFormatException, IOException {
		final Set<String> genes = new THashSet<String>();
		PCTSEA.logStatus("Reading experimental expressions from file "
				+ FilenameUtils.getName(experimentalExpressionsFile.getAbsolutePath()));
		BufferedReader reader = null;
		int id = 0;

		try {
			reader = new BufferedReader(new FileReader(experimentalExpressionsFile));
			String line = null;
			int numLine = 0;
			while ((line = reader.readLine()) != null) {
				ConcurrentUtil.sleep(1L);
				if ("".equals(line.trim())) {
					continue;
				}
				final String[] split = line.split("\t");
				numLine++;
				if (numLine == 1) {
					continue;
				}
				final String geneName = split[0].toUpperCase();
				final int geneID = id++;
				genes.add(geneName);
				InteractorsExpressionsRetriever.geneIDsByGeneNameMap.put(geneName, geneID);
				InteractorsExpressionsRetriever.geneNamesByGeneIDMap.put(geneID, geneName);
				final float interactorExpressionInOurExperiment = Float.valueOf(split[1]);
				interactorExpressionsInOurExperiment.put(geneID, interactorExpressionInOurExperiment);

			}
		} finally {
			reader.close();
			final String message = "Information from " + interactorExpressionsInOurExperiment.size()
					+ " genes/proteins readed";
			PCTSEA.logStatus(message);
		}
		return genes.stream().collect(Collectors.toList());
	}

	public Gene getExpressionsOfGene(int geneID) {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}
		return genesById.get(geneID);
	}

	public TIntList getInteractorsGeneIDs() {

		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}
		return geneIDs;
	}

	public float getInteractionExpressionInOurExperiment(int geneID) {
		return interactorExpressionsInOurExperiment.get(geneID);
	}

	public void permuteSingleCellExpressions(List<SingleCell> totalSingles) {

		final boolean problemOccurred = genesById.forEachValue(gene -> gene.permuteGeneExpressionInCells(totalSingles));
		if (problemOccurred) {
			throw new RuntimeException("Error while permuting gene expresssions in cells");
		}
	}

	public void permuteSingleCellExpressions() {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}
		for (final Gene gene : genesById.valueCollection()) {
			final boolean noError = gene.permuteGeneExpressionInCells();
			if (!noError) {
				throw new RuntimeException(
						"Error while permuting gene expresssions in cells for gene " + gene.getGeneName());
			}
		}

	}

	public String getGeneName(int geneID) {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}
		return geneNamesByGeneIDMap.get(geneID);
	}

	public int getGeneID(String geneName) {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}
		return geneIDsByGeneNameMap.get(geneName);
	}

	/**
	 * Generates a unique key with the sorted list of genes. This key will be used
	 * by the tool to avoid to calculate the correlations again
	 * 
	 * @return
	 */
	public String getInteractorsGeneNamesKey() {

		Collections.sort(genes);
		return StringUtils.getSortedSeparatedValueStringFromChars(genes, "-");
	}

	public static void setInstance(InteractorsExpressionsRetriever interactorExpressions) {
		instance = interactorExpressions;
	}
}
