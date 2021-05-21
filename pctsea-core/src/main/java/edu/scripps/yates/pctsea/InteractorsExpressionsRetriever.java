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
import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;

import com.mongodb.MongoException;

import edu.scripps.yates.annotations.uniprot.UniprotProteinLocalRetriever;
import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLog;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.Gene;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import edu.scripps.yates.utilities.annotations.uniprot.UniprotEntryUtil;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.pi.ConcurrentUtil;
import edu.scripps.yates.utilities.strings.StringUtils;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.TShortFloatMap;
import gnu.trove.map.TShortObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.map.hash.TShortFloatHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TShortHashSet;

public class InteractorsExpressionsRetriever {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(InteractorsExpressionsRetriever.class);
	private final TShortObjectMap<Gene> genesById = new TShortObjectHashMap<Gene>();
	private final TShortFloatMap interactorExpressionsInOurExperiment = new TShortFloatHashMap();
	private final TShortList geneIDs = new TShortArrayList();
	private final List<String> genes;
	private static final TObjectShortMap<String> geneIDsByGeneNameMap = new TObjectShortHashMap<String>();
	private static final TShortObjectMap<String> geneNamesByGeneIDMap = new TShortObjectHashMap<String>();
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

//		PCTSEA.logStatus(geneIDsByGeneNameMap.size() + " - " + geneNamesByGeneIDMap.size());
		final TIntSet singleCellIDs = new TIntHashSet();
		final List<String> genesFound = new ArrayList<String>();

		final Map<String, List<String>> genesByInputEntry = getGenesFromInputList(inputProteinGeneList);
		final Map<String, List<String>> inputEntryByGene = new THashMap<String, List<String>>();
		final Set<String> totalGenes = new THashSet<String>();
		for (final String inputEntry : genesByInputEntry.keySet()) {
			final List<String> genes = genesByInputEntry.get(inputEntry);

			for (final String gene : genes) {
				totalGenes.add(gene);
				if (!inputEntryByGene.containsKey(gene)) {
					inputEntryByGene.put(gene, new ArrayList<String>());
				}
				inputEntryByGene.get(gene).add(inputEntry);
			}

		}
		final Set<String> foundGeneNames = new THashSet<String>();
		final Set<String> foundInputEntries = new THashSet<String>();
		final TShortSet geneIDsSet = new TShortHashSet();
		PCTSEA.logStatus("Getting single-cell expression profiles with the input protein/gene list...");

		final DocumentCallbackHandler documentProcessor = new DocumentCallbackHandler() {

			@Override
			public void processDocument(Document doc) throws MongoException, DataAccessException {
				final Object cellTypeObj = doc.get("cellType");
				if (cellTypeObj == null) {
					// ignore it if it doesnt have a type
					return;
				}
				final String cellNameObj = (String) doc.get("cellName");
				String cellName = null;
				if (cellNameObj != null) {
					cellName = cellNameObj.toString();
				}

				final String geneObj = (String) doc.get("gene");
				String geneName = null;
				if (geneObj != null) {
					geneName = geneObj.toString();
				}
				final Double expressionObj = (Double) doc.get("expression");
				float expression = Float.NaN;
				if (expressionObj != null) {
					expression = Float.valueOf(expressionObj.toString());
				}

				foundGeneNames.add(geneName);
				// it already has an id associated from the call to getGenesFromInputList
				final short geneID = geneIDsByGeneNameMap.get(geneName.toUpperCase());
				if (!genesById.containsKey(geneID)) {
					final Gene gene = new Gene(geneID, geneName);
					genesById.put(geneID, gene);
					geneIDsSet.add(geneID);
				}

				final int singleCellID = SingleCellsMetaInformationReader.getSingleCellIDBySingleCellName(cellName);
				singleCellIDs.add(singleCellID);
				final SingleCell cell = SingleCellsMetaInformationReader.getSingleCellByCellID(singleCellID);
				cell.addGeneExpressionValue(geneID, expression);
				final String cellTypeName = cell.getCellType(cellTypeBranch);
				final Gene gene = getExpressionsOfGene(geneID);
				gene.addExpressionValueInSingleCell(singleCellID, expression, cellTypeName);
			}
		};
		final long t0 = System.currentTimeMillis();
		mongoBaseService.getExpressionByGenes(totalGenes, dataset, documentProcessor);

		final long t1 = System.currentTimeMillis();
		System.out.println("Expressions retrieved in " + DatesUtil.getDescriptiveTimeFromMillisecs(t1 - t0));
//		final List<Expression> totalExpressions = mongoBaseService.getExpressionByGenes(totalGenes, dataset);
//		final long t2 = System.currentTimeMillis();
//		System.out.println(totalExpressions.size() + " in " + DatesUtil.getDescriptiveTimeFromMillisecs(t2 - t1));

		// in foundGeneNames we have all the genes found...lets see if we have all
		// inputEntries
		for (final String foundGeneName : foundGeneNames) {
			final List<String> inputEntries = inputEntryByGene.get(foundGeneName);
			if (inputEntries != null) {
				foundInputEntries.addAll(inputEntries);
			}
		}
		// now we look at the ones that are not found
		final List<String> notFoundInputEntries = new ArrayList<String>();
		for (final String inputEntry : inputProteinGeneList) {
			if (!foundInputEntries.contains(inputEntry)) {
				notFoundInputEntries.add(inputEntry);
			}
		}
		// log num input genes that are not found
		runLog.setInputGenesNotFound(notFoundInputEntries);
		if (!notFoundInputEntries.isEmpty()) {
			final String message2 = "Expression values from " + genesFound.size()
					+ " input entries were found in the database. Expression values from " + notFoundInputEntries.size()
					+ " genes were NOT found in the single cells dataset:";
			PCTSEA.logStatus(message2);
//					System.out.println(message2);
			String message3 = "";
			for (final String geneNotFound : notFoundInputEntries) {
				message3 += geneNotFound + ",";
			}
			PCTSEA.logStatus(message3);
//					System.out.println(message3);
		}

		geneIDs.addAll(geneIDsSet);

//		System.out.println(message);

		final String message = singleCellIDs.size() + " single cells with expression values in " + genesById.size()
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
				final short geneID = geneIDsByGeneNameMap.get(uniprotAcc);
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
		short id = 0;

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
				final short geneID = id++;
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

	public Gene getExpressionsOfGene(short geneID) {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}
		return genesById.get(geneID);
	}

	public TShortList getInteractorsGeneIDs() {

		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}

		return geneIDs;
	}

	public float getInteractionExpressionInOurExperiment(short geneID) {
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

	public String getGeneName(short geneID) {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes);
		}
		return geneNamesByGeneIDMap.get(geneID);
	}

	public short getGeneID(String geneName) {
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
