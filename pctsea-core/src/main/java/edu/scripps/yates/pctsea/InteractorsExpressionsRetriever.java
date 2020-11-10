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
import org.apache.log4j.Logger;

import edu.scripps.yates.annotations.uniprot.UniprotProteinLocalRetriever;
import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.model.Gene;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import edu.scripps.yates.utilities.annotations.uniprot.UniprotEntryUtil;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.fasta.FastaParser;
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
	private final static Logger log = Logger.getLogger(InteractorsExpressionsRetriever.class);
	private final TIntObjectMap<Gene> genesById = new TIntObjectHashMap<Gene>();
	private final TIntFloatMap interactorExpressionsInOurExperiment = new TIntFloatHashMap();
	private final TIntList geneIDs = new TIntArrayList();
	private final List<String> genes;
	private static final TObjectIntMap<String> geneIDsByGeneNameMap = new TObjectIntHashMap<String>();
	private static final TIntObjectMap<String> geneNamesByGeneIDMap = new TIntObjectHashMap<String>();
	private final ExpressionMongoRepository expresssionsMongoRepository;
	private final String projectTag;
	private static InteractorsExpressionsRetriever instance;

	/**
	 * 
	 * @param expresssionsMongoRepository
	 * @param experimentalExpressionsFile
	 * @param minNumInteractorsForCorrelation
	 * @throws IOException
	 */
	public InteractorsExpressionsRetriever(ExpressionMongoRepository expresssionsMongoRepository,
			File experimentalExpressionsFile, String projectTag) throws IOException {
//		if (SingleCellsMetaInformationReader.singleCellIDsBySingleCellNameMap.isEmpty()) {
//			throw new IllegalArgumentException(
//					"We need to read the single cell metainformation before reading expressions");
//		}
		// clear static
		geneIDsByGeneNameMap.clear();
		geneNamesByGeneIDMap.clear();
		this.projectTag = projectTag;

		this.expresssionsMongoRepository = expresssionsMongoRepository;
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
	 * @param projectTag
	 * @throws IOException
	 */
	private void getSingleCellExpressionsFromDB(List<String> inputProteinGeneList, ExpressionMongoRepository repo,
			String projectTag) {
		log.info("Getting single-cell expression profiles with the input protein/gene list...");
		System.out.println(geneIDsByGeneNameMap.size() + " - " + geneNamesByGeneIDMap.size());
		final Set<String> singleCellNames = new THashSet<String>();
		final List<String> genesNotFound = new ArrayList<String>();
		final ProgressCounter counter = new ProgressCounter(inputProteinGeneList.size(),
				ProgressPrintingType.PERCENTAGE_STEPS, 0, true);
		counter.setSuffix("Getting expression profiles of interest...");

		// get annotations from uniprot so I can map to gene names and their synonyms
		final Map<String, Entry> annotatedProteins = new THashMap<String, Entry>();
		final Set<String> uniprotAccs = inputProteinGeneList.stream().filter(p -> FastaParser.isUniProtACC(p))
				.collect(Collectors.toSet());
		if (!uniprotAccs.isEmpty()) {
			final UniprotProteinLocalRetriever uplr = new UniprotProteinLocalRetriever(
					new File(System.getProperty("user.dir")), true);
			annotatedProteins.putAll(uplr.getAnnotatedProteins(null, uniprotAccs));
		}
		// first we have to figure out if the input list are uniprot names, and map then
		// to the appropriate gene name that is present in the dataset
		for (final String inputProteinGene : inputProteinGeneList) {
			counter.increment();
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				log.info(printIfNecessary);
			}
			int geneID = 0;
			String geneName = inputProteinGene;
			List<Expression> expressions = null;
			if (uniprotAccs.contains(inputProteinGene)) {
				expressions = repo.findByGene(inputProteinGene);
				if (expressions == null || expressions.isEmpty()) {
					// look for gene names from uniprot
					if (annotatedProteins.containsKey(inputProteinGene)) {
						final Entry entry = annotatedProteins.get(inputProteinGene);
						if (entry != null) {
							final List<String> geneNames = getGeneNames(
									UniprotEntryUtil.getGeneName(entry, false, true));
							for (final String geneName2 : geneNames) {
								expressions = repo.findByGene(geneName2);
								if (expressions != null && !expressions.isEmpty()) {
									// change map between geneID and geneName
									geneID = geneIDsByGeneNameMap.get(inputProteinGene);
									geneIDsByGeneNameMap.put(geneName2, geneID);
									geneIDsByGeneNameMap.remove(inputProteinGene);
									geneNamesByGeneIDMap.put(geneID, geneName2);
									geneName = geneName2;
									break;
								}
							}
						}
					}
				}
			} else {
				expressions = repo.findByGene(geneName);
				geneID = geneIDsByGeneNameMap.get(geneName);
			}
			if (expressions == null || expressions.isEmpty()) {
				genesNotFound.add(inputProteinGene);
				continue;
			}

			final Gene gene = new Gene(geneID, geneName);
			genesById.put(geneID, gene);
			geneIDs.add(geneID);
			for (final Expression expression : expressions) {
				if (projectTag != null && !projectTag.equals(expression.getProjectTag())) {
//					System.out.println(projectID + " different than " + expression.getProject().getId());
					continue;
				}
				final float interactorExpressionInSingleCell = expression.getExpression();
				final String singleCellName = expression.getCellName();
				singleCellNames.add(singleCellName);
				final int singleCellID = SingleCellsMetaInformationReader
						.getSingleCellIDBySingleCellName(singleCellName);

				final SingleCell cell = SingleCellsMetaInformationReader.getSingleCellByCellID(singleCellID);

				cell.addGeneExpressionValue(geneID, interactorExpressionInSingleCell);
				gene.addExpressionValueInSingleCell(singleCellID, interactorExpressionInSingleCell);
			}

		}

		final String message = "Expression values from " + singleCellNames.size() + " single cells in "
				+ genesById.size() + " genes/proteins out of a total "
				+ SingleCellsMetaInformationReader.getNumSingleCells() + " of single cells";
		log.info(message);
//		System.out.println(message);

		if (!genesNotFound.isEmpty()) {
			final String message2 = "Expression values from " + genesNotFound.size()
					+ " genes were not found in the single cells dataset:";
			log.info(message2);
//			System.out.println(message2);
			String message3 = "";
			for (final String geneNotFound : genesNotFound) {
				message3 += geneNotFound + ",";
			}
			log.info(message3);
//			System.out.println(message3);
		}

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
		log.info("Reading experimental expressions from file "
				+ FilenameUtils.getName(experimentalExpressionsFile.getAbsolutePath()));
		BufferedReader reader = null;
		int id = 0;

		try {
			reader = new BufferedReader(new FileReader(experimentalExpressionsFile));
			String line = null;
			int numLine = 0;
			while ((line = reader.readLine()) != null) {
				if ("".equals(line.trim())) {
					continue;
				}
				final String[] split = line.split("\t");
				numLine++;
				if (numLine == 1) {
					continue;
				}
				final String geneName = split[0];
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
			log.info(message);
		}
		return genes.stream().collect(Collectors.toList());
	}

	public Gene getExpressionsOfGene(int geneID) {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes, expresssionsMongoRepository, projectTag);
		}
		return genesById.get(geneID);
	}

	public TIntList getInteractorsGeneIDs() {

		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes, expresssionsMongoRepository, projectTag);
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
			getSingleCellExpressionsFromDB(genes, expresssionsMongoRepository, projectTag);
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
			getSingleCellExpressionsFromDB(genes, expresssionsMongoRepository, projectTag);
		}
		return geneNamesByGeneIDMap.get(geneID);
	}

	public int getGeneID(String geneName) {
		if (genesById.isEmpty()) {
			getSingleCellExpressionsFromDB(genes, expresssionsMongoRepository, projectTag);
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
