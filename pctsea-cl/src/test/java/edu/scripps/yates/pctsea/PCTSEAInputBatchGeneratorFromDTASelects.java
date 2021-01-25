package edu.scripps.yates.pctsea;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.tools.ant.DirectoryScanner;

import edu.scripps.yates.annotations.uniprot.UniprotProteinLocalRetriever;
import edu.scripps.yates.dtaselectparser.DTASelectParser;
import edu.scripps.yates.utilities.annotations.uniprot.UniprotEntryUtil;
import edu.scripps.yates.utilities.annotations.uniprot.xml.Entry;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.grouping.GroupableProtein;
import edu.scripps.yates.utilities.grouping.PAnalyzer;
import edu.scripps.yates.utilities.grouping.ProteinEvidence;
import edu.scripps.yates.utilities.grouping.ProteinGroup;
import edu.scripps.yates.utilities.proteomicsmodel.Protein;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.map.hash.THashMap;

/**
 * Generates a set of input files for PCTSEA from a set of DTASelects in a
 * folder, grouping them by a criteria because they are replicates.
 * 
 * @author salvador
 *
 */
public class PCTSEAInputBatchGeneratorFromDTASelects {
	private static final UniprotProteinLocalRetriever uplr = new UniprotProteinLocalRetriever(
			new File("Z:\\share\\Salva\\UniprotKB"), true);

	public static void main(String[] args) {
		try {
			File folder = new File("Z:\\share\\Salva\\data\\cbamberg\\NCI60_cell_lines\\all_data_092019");
			String filesRegexp = "DTASelect-filter*";
			String cellLineRegexp = "_(Line_\\d+)_";
			if (args.length == 3) {
				folder = new File(args[0]);
				filesRegexp = args[1];
				cellLineRegexp = args[2];
			}
			final PCTSEAInputBatchGeneratorFromDTASelects r = new PCTSEAInputBatchGeneratorFromDTASelects(folder,
					filesRegexp, cellLineRegexp);
			r.run();
			System.out.println("Everything ok!");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private final File dtaSelectFolder;
	private final String filesRegexp;
	private final String cellLineRegexp;

	public PCTSEAInputBatchGeneratorFromDTASelects(File folder, String filesRegexp, String cellLineRegexp) {
		dtaSelectFolder = folder;
		this.filesRegexp = filesRegexp;
		this.cellLineRegexp = cellLineRegexp;
	}

	public void run() throws IOException {
		final Map<String, List<File>> groupsOfFiles = getFilesByExperiments(dtaSelectFolder, filesRegexp,
				cellLineRegexp);
		final List<String> cellLineIDs = groupsOfFiles.keySet().stream().sorted().collect(Collectors.toList());
		System.out.println(cellLineIDs.size() + " cell lines");
		for (final String cellLineID : cellLineIDs) {
			final File outputFile = new File(
					dtaSelectFolder.getAbsolutePath() + File.separator + "pctsea_input_" + cellLineID + ".txt");
			final FileWriter fw = new FileWriter(outputFile);
			final List<File> list = groupsOfFiles.get(cellLineID);
			final DTASelectParser parser = new DTASelectParser(list);
			parser.setDecoyPattern("Reverse");
			parser.setIgnoreACCFormat(true);
			parser.setIgnoreTaxonomies(true);
			final Map<String, Entry> uniprotEntries = uplr.getAnnotatedProteins(null, parser.getProteinMap().keySet());
			final List<Protein> proteins = parser.getProteins();
			final PAnalyzer panalyzer = new PAnalyzer(true);
			final List<GroupableProtein> groupableProteins = new ArrayList<GroupableProtein>();
			proteins.forEach(p -> {
				final String uniprotAcc = FastaParser.getUniProtACC(p.getAccession());
				if (uniprotAcc != null) {
					p.setPrimaryAccession(uniprotAcc);
				}
				groupableProteins.add(p);
			});
			final List<ProteinGroup> proteinGroups = panalyzer.run(groupableProteins);
			for (final ProteinGroup proteinGroup : proteinGroups) {
				if (proteinGroup.getEvidence() == ProteinEvidence.NONCONCLUSIVE) {
					continue;
				}

				final int spc = proteinGroup.getPSMs().size();

				final List<String> accessionsFilteredByEvidence = proteinGroup
						.getAccessionsFilteredByEvidence(uniprotEntries, ",");
				if (accessionsFilteredByEvidence.size() > 1) {
					final Map<String, List<String>> mapToGenes = mapAccessionsToGenes(accessionsFilteredByEvidence,
							uniprotEntries);
					for (final String gene : mapToGenes.keySet()) {
						// per gene, we choose one
						final String acc = mapToGenes.get(gene).get(0);
						fw.write(acc + "\t" + spc + "\t" + gene + "\n");
					}
				} else {
					final String gene = getPrimaryGene(accessionsFilteredByEvidence.get(0), uniprotEntries);
					fw.write(accessionsFilteredByEvidence.get(0) + "\t" + spc + "\t" + gene + "\n");
				}

			}
			fw.close();
			System.out.println("File written at: " + outputFile.getAbsolutePath());
		}

	}

	private String getPrimaryGene(String acc, Map<String, Entry> uniprotEntries) {
		final Entry entry = uniprotEntries.get(acc);
		final List<Pair<String, String>> genes = UniprotEntryUtil.getGeneName(entry, true, true);
		if (genes != null && !genes.isEmpty()) {
			final String gene = genes.get(0).getFirstelement();
			return gene;
		}
		return "N/A";
	}

	private Map<String, List<String>> mapAccessionsToGenes(List<String> accs, Map<String, Entry> uniprotEntries) {
		final Map<String, List<String>> ret = new THashMap<String, List<String>>();
		for (final String acc : accs) {
			if (uniprotEntries.containsKey(acc)) {
				final Entry entry = uniprotEntries.get(acc);
				final List<Pair<String, String>> genes = UniprotEntryUtil.getGeneName(entry, true, true);
				if (genes != null && !genes.isEmpty()) {
					final String gene = genes.get(0).getFirstelement();
					if (!ret.containsKey(gene)) {
						ret.put(gene, new ArrayList<String>());
					}
					ret.get(gene).add(acc);
				} else {
					if (!ret.containsKey(acc)) {
						ret.put(acc, new ArrayList<String>());
					}
					ret.get(acc).add(acc);
				}
			} else {
				if (!ret.containsKey(acc)) {
					ret.put(acc, new ArrayList<String>());
				}
				ret.get(acc).add(acc);
			}
		}
		return ret;
	}

	private Map<String, List<File>> getFilesByExperiments(File dtaSelectFolder2, String filesRegexp,
			String cellLineRegexp) {
		final Map<String, List<File>> ret = new THashMap<String, List<File>>();
		final DirectoryScanner scanner = new DirectoryScanner();
		scanner.setIncludes(new String[] { filesRegexp });
		scanner.setBasedir(dtaSelectFolder2);
		scanner.setCaseSensitive(true);
		scanner.scan();
		final String[] includedFiles = scanner.getIncludedFiles();
		final Pattern pattern = Pattern.compile(cellLineRegexp);

		for (final String filePath : includedFiles) {
			final Matcher matcher = pattern.matcher(filePath);
			if (matcher.find()) {
				final String cellLineID = matcher.group(1);
				if (!ret.containsKey(cellLineID)) {
					ret.put(cellLineID, new ArrayList<File>());
				}
				ret.get(cellLineID).add(new File(dtaSelectFolder2 + File.separator + filePath));
			}
		}
		return ret;

	}
}
