package edu.scripps.yates.pctsea.comparator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.pctsea.model.CellTypeClassification;
import edu.scripps.yates.pctsea.utils.CellTypesOutputTableColumns;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

/**
 * After generating the input for PCTSEA in
 * {@link PCTSEAInputBatchGeneratorFromDTASelects}, we got the results, that is,
 * 60 zip files.<br>
 * This class is going to read a set of zip files that are results from pctsea
 * and it will create a table in which each column will be a result and each
 * row, a significantly enriched cell type and each cell of the matrix, the
 * enrichment score or p-value
 * 
 * @author salvador
 *
 */
public class PCTSEAMultipleResultsMerger {
	private final Logger log = Logger.getLogger(PCTSEAMultipleResultsMerger.class);
	public static final String resultsZipFolderPathLocal = "D:\\Dropbox (Scripps Research)\\NCI60_pctsea";
	private final List<File> zipFiles;
	private final Double fdrThreshold;
	private final File comparisonOutputFolder;

	public PCTSEAMultipleResultsMerger(List<File> zipFiles, File comparisonOutputFolder) {
		this(zipFiles, null, comparisonOutputFolder);
	}

	public PCTSEAMultipleResultsMerger(List<File> zipFiles, Double fdrThreshold, File comparisonOutputFolder) {
		this.zipFiles = zipFiles;
		this.fdrThreshold = fdrThreshold;
		this.comparisonOutputFolder = comparisonOutputFolder;
	}

//	public static void main(String args[]) {
//		final CellTypesOutputTableColumns[] values = CellTypesOutputTableColumns.values();
//		for (final CellTypesOutputTableColumns cellTypesOutputTableColumns : values) {
//			System.out.print("'" + cellTypesOutputTableColumns.getColumnName() + "',");
//		}
//		System.out.println();
//		final File resultsZipFolder = new File(resultsZipFolderPathLocal);
//		final double fdrThreshold = 1;
//		final PCTSEAMultipleResultsMerger nci60resultsMerger = new PCTSEAMultipleResultsMerger(resultsZipFolder,
//				fdrThreshold);
//		try {
//			final CellTypesOutputTableColumns[] cols = values;
//			for (final CellTypesOutputTableColumns col : cols) {
//				nci60resultsMerger.run(col);
//			}
//
//		} catch (final Exception e) {
//			e.printStackTrace();
//		}
//	}

	public void run(CellTypesOutputTableColumns columnToPrint) throws ZipException, IOException {
		log.info("Processing results for " + columnToPrint.getColumnName());

//		final File[] resultsFiles = getResultFiles(resultsZipFolderPath);
		final Set<String> cellTypeNames = new THashSet<String>(); // to keep the total set of cell types across the
		// experiments
		final Map<String, Map<String, CellTypeClassification>> cellTypesByExperiment = new THashMap<String, Map<String, CellTypeClassification>>();
		final ProgressCounter counter = new ProgressCounter(zipFiles.size(), ProgressPrintingType.EVERY_STEP, 0, true);
		counter.setSuffix("reading result files for " + columnToPrint.getColumnName() + "...");
		for (final File resultsFile : zipFiles) {
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				log.info(printIfNecessary);
			}
			final Pair<String, Map<String, CellTypeClassification>> pair = processResultsFile(resultsFile);
			final String experimentName = pair.getFirstelement();
			final Map<String, CellTypeClassification> cellTypes = pair.getSecondElement();
			cellTypes.values().stream().forEach(c -> cellTypeNames.add(c.getName()));
			cellTypesByExperiment.put(experimentName, cellTypes);
			counter.increment();

		}
		if (fdrThreshold != null) {
			log.info(cellTypeNames.size() + " significant cell types (FDR < " + fdrThreshold + ") from "
					+ cellTypesByExperiment.size() + " experiments");
		} else {
			log.info(cellTypeNames.size() + " cell types from " + cellTypesByExperiment.size() + " experiments");
		}
		// now we have the map with all the data from all experiments, we print the
		// table
		printTable(cellTypeNames, cellTypesByExperiment, columnToPrint);
	}

	private void printTable(Set<String> cellTypeNames,
			Map<String, Map<String, CellTypeClassification>> cellTypesByExperiment,
			CellTypesOutputTableColumns columnToPrint) throws IOException {
		// get, for each cell type, how many experiments is significant in
		final TObjectIntMap<String> numExperimentsPerCellType = new TObjectIntHashMap<String>();
		for (final Map<String, CellTypeClassification> cellTypes : cellTypesByExperiment.values()) {
			for (final CellTypeClassification cellType : cellTypes.values()) {
				final String cellTypeName = cellType.getName();
				if (numExperimentsPerCellType.containsKey(cellTypeName)) {
					numExperimentsPerCellType.put(cellTypeName, numExperimentsPerCellType.get(cellTypeName) + 1);
				} else {
					numExperimentsPerCellType.put(cellTypeName, 1);
				}
			}
		}
		final List<String> cellTypeNamesList = cellTypeNames.stream().collect(Collectors.toList());
		// sort by the cell types that are present in more of the experiments
		Collections.sort(cellTypeNamesList, new Comparator<String>() {

			@Override
			public int compare(String cellTypeName1, String cellTypeName2) {
				return Integer.compare(numExperimentsPerCellType.get(cellTypeName2),
						numExperimentsPerCellType.get(cellTypeName1));
			}
		});
		final String outputFileName = comparisonOutputFolder.getAbsolutePath() + File.separator
				+ "SignificantCellTypesTable_" + columnToPrint.getColumnName() + ".txt";
		// now we print the table
		final FileWriter fw = new FileWriter(outputFileName);
		// header: experiments
		final List<String> experimentNameList = cellTypesByExperiment.keySet().stream().sorted()
				.collect(Collectors.toList());
		for (final String experimentName : experimentNameList) {
			final String expNumber = experimentName.substring("NCI60_".length());
			fw.write("\t" + expNumber);
		}
		fw.write("\n");
		for (final String cellTypeName : cellTypeNamesList) {

			fw.write(cellTypeName);
			for (final String experimentName : experimentNameList) {
				fw.write("\t");
				final Map<String, CellTypeClassification> cellTypesByName = cellTypesByExperiment.get(experimentName);
				if (cellTypesByName.containsKey(cellTypeName)) {
					final CellTypeClassification cellType = cellTypesByName.get(cellTypeName);
					fw.write(columnToPrint.getValue(cellType, -1, -1, null));
				}
			}
			fw.write("\n");
		}
		fw.close();
		log.info("File created: " + outputFileName);
	}

	private Pair<String, Map<String, CellTypeClassification>> processResultsFile(File resultsFile)
			throws ZipException, IOException {

		Pair<String, Map<String, CellTypeClassification>> ret = null;
		final ZipFile zipFile = new ZipFile(resultsFile);

		final Enumeration<? extends ZipEntry> entries = zipFile.getEntries();

		final String beginingOfHeader = CellTypesOutputTableColumns.values()[0].getColumnName() + "\t"
				+ CellTypesOutputTableColumns.values()[1].getColumnName();

		while (entries.hasMoreElements()) {
			final ZipEntry entry = entries.nextElement();
			final String entryName = entry.getName();
			if (!entryName.endsWith("cell_types_enrichment.txt")) {
				continue;
			}
			String experimentName = entryName.substring(0, entryName.indexOf("_cell_types_enrichment.txt"));
			if (experimentName.contains("/")) {
				experimentName = experimentName.substring(experimentName.indexOf("/") + 1);
			}
			final Map<String, CellTypeClassification> cellTypes = new THashMap<String, CellTypeClassification>();
			ret = new Pair<String, Map<String, CellTypeClassification>>(experimentName, cellTypes);
			final InputStream stream = zipFile.getInputStream(entry);
			// get lines
			final List<String> fileLines = getLinesFromFile(stream);
			boolean tableLine = false; // determines when the actual table starts
			for (final String line : fileLines) {
				if (!tableLine) {
					if (line.startsWith(beginingOfHeader)) {
						tableLine = true;
						continue;
					} else {
						continue;
					}
				} else if (line.startsWith(PCTSEA.NEGATIVE_EWS_FROM_HERE)) {
					break;
				}
				final CellTypeClassification cellType = CellTypesOutputTableColumns.getCellTypeFromLine(line, "\t");
				final double enrichmentFDR = cellType.getEnrichmentFDR();
				if (fdrThreshold != null && enrichmentFDR > fdrThreshold) {
					continue;
				}

				cellTypes.put(cellType.getName(), cellType);
			}
		}

		zipFile.close();
		return ret;
	}

	private List<String> getLinesFromFile(InputStream stream) {
		final List<String> lines = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.toList());
		try {
			stream.close();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // is this failing?
		return lines;
	}

	private File[] getResultFiles(File resultsZipFolderPath2) {
		final File[] ret = resultsZipFolderPath2.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				final String extension = FilenameUtils.getExtension(name);
				if (extension.equals("zip")) {
					return true;
				}
				return false;
			}
		});
		return ret;
	}
}
