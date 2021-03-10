package edu.scripps.yates.pctsea.views.analyze;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.scripps.yates.annotations.uniprot.UniprotProteinLocalRetriever;
import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.utilities.annotations.uniprot.UniprotEntryUtil;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.map.hash.THashMap;

public class InputDataMappingValidation {

	public Map<String, Pair<String, Long>> mapToDatabase(List<String> inputProteinGeneList, String uniprotRelease,
			MongoBaseService mongoBaseService, String dataset) {

		final Map<String, Pair<String, Long>> ret = new THashMap<String, Pair<String, Long>>();
		final Map<String, List<String>> genesByInputEntry = getGenesFromInputList(inputProteinGeneList, uniprotRelease);

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

				final long count = mongoBaseService.countExpressionsByGeneAndDatasets(geneName, dataset);
				if (count == 0l) {
					continue; // we try the next one
				}

				ret.put(inputProteinGene, new Pair<String, Long>(geneName, count));
				inputProteinGeneFound = true;
				break;
			}
			if (!inputProteinGeneFound) {
				ret.put(inputProteinGene, null);
			}
		}
		return ret;
	}

	/**
	 * Returns a map for each input element, mapped to a list of genes
	 * 
	 * @param inputProteinGeneList
	 * @return
	 */
	private Map<String, List<String>> getGenesFromInputList(List<String> inputProteinGeneList, String uniprotRelease) {
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

				if (annotatedProteins.containsKey(uniprotAcc)) {
					final Entry entry = annotatedProteins.get(uniprotAcc);
					if (entry != null) {
						final List<String> geneNames = getGeneNames(UniprotEntryUtil.getGeneName(entry, false, true));
						for (final String geneName2 : geneNames) {
							genesByInputEntry.get(uniprotAcc).add(geneName2.toUpperCase());
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
}
