package edu.scripps.yates.pctsea.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.boot.logging.LogLevel;

import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class SingleCellsMetaInformationReader {
	private static final Logger log = Logger.getLogger(SingleCellsMetaInformationReader.class);
	private static final TIntObjectMap<SingleCell> singleCellsByCellID = new TIntObjectHashMap<SingleCell>();
	private static final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
	private static final TObjectIntMap<String> singleCellIDsBySingleCellNameMap = new TObjectIntHashMap<String>();
	private static final TIntList cellIDs = new TIntArrayList();
	private static int totalNumCellsForDataset;

//	/**
//	 * This reads a file with the meta-information of the single cells, their
//	 * classifications, etc...
//	 * 
//	 * @param metadataFile
//	 * @throws IOException
//	 */
//	public SingleCellsMetaInformationReader(File metadataFile) throws IOException {
//		PCTSEA.logStatus("Reading single cells metadata file: " + FilenameUtils.getBaseName(metadataFile.getAbsolutePath()));
//		if (!metadataFile.exists()) {
//			throw new FileNotFoundException(metadataFile.getAbsolutePath() + " not found");
//		}
//		int cellID = 0;
//		BufferedReader reader = null;
//		try {
//			reader = new BufferedReader(new FileReader(metadataFile));
//			String line = null;
//			final TObjectIntMap<String> indexesByHeader = new TObjectIntHashMap<String>();
//			while ((line = reader.readLine()) != null) {
//				final String[] split = line.split("\t");
//				if (indexesByHeader.isEmpty()) {
//					int i = 0;
//					for (final String string : split) {
//						indexesByHeader.put(string.toLowerCase(), i++);
//					}
//				} else {
//					final String cellName = split[indexesByHeader.get("cell_id")];
//					final String cellType = split[indexesByHeader.get("celltype")].trim().toLowerCase();
//					cellID++;
//					singleCellIDsBySingleCellNameMap.put(cellName, cellID);
//					singleCellNamesBySingleCellIDMap.put(cellID, cellName);
//					cellIDs.add(cellID);
//					final SingleCell singleCell = new SingleCell(cellID, cellName, Double.NaN);
//
//					singleCell.setCellType(cellType);
//
//					// in case of having more columns (not in human cell map)
//					if (indexesByHeader.containsKey("developmentstage")) {
//						final String developmentStage = split[indexesByHeader.get("developmentstage")];
//						singleCell.setDevelopmentStage(developmentStage);
//					}
//					if (indexesByHeader.containsKey("gender")) {
//						final String gender = split[indexesByHeader.get("gender")];
//						singleCell.setGender(gender);
//					}
//					if (indexesByHeader.containsKey("biomaterial")) {
//						final String biomaterial = split[indexesByHeader.get("biomaterial")];
//						singleCell.setBiomaterial(biomaterial);
//					}
//					if (indexesByHeader.containsKey("ages")) {
//						final String age = split[indexesByHeader.get("ages")];
//						singleCell.setAge(age);
//					}
//
//					addSingleCell(singleCell);
//
//				}
//			}
//		} finally {
//			reader.close();
//			final String message = "Information from " + singleCellList.size() + " single cells read";
//			PCTSEA.logStatus(message);
////			System.out.println(message);
//		}
//	}

	/**
	 * This reads a file with the meta-information of the single cells, their
	 * classifications, etc...
	 * 
	 * @param metadataFile
	 * @throws IOException
	 */
	public SingleCellsMetaInformationReader(File annotationRMBatchFolder) throws IOException {
		PCTSEA.logStatus(
				"Reading single cells metadata information from folder: " + annotationRMBatchFolder.getAbsolutePath());
		final File[] files = annotationRMBatchFolder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (FilenameUtils.getExtension(name).equals("csv")) {
					return true;
				}
				return false;
			}
		});
		// get total size
		long totalSize = 0;
		for (int numFile = 1; numFile <= files.length; numFile++) {
			totalSize += files[numFile - 1].length();
		}
		PCTSEA.logStatus(FileUtils.getDescriptiveSizeFromBytes(totalSize) + " from all files");
		final ProgressCounter counter = new ProgressCounter(totalSize, ProgressPrintingType.EVERY_STEP, 1, true);
		int cellID = 0;
		final int cellsNotFound = 0;
		for (final File metadataFile : files) {

			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(metadataFile));
				String line = null;
				final TObjectIntMap<String> indexesByHeader = new TObjectIntHashMap<String>();
				while ((line = reader.readLine()) != null) {
					final String[] split = line.split(",");
					if (indexesByHeader.isEmpty()) {
						int i = 0;
						for (final String string : split) {
							String header = string.toLowerCase().trim();
							if (header.startsWith("\"")) {
								header = header.replace("\"", "");
							}
							indexesByHeader.put(header, i++);
						}
					} else {
						if (!indexesByHeader.containsKey("cell_id")) {
							PCTSEA.logStatus("File " + metadataFile.getAbsolutePath() + " has different header!",
									LogLevel.ERROR);
						}
						final int cellIDIndex = indexesByHeader.get("cell_id");
						if (cellIDIndex >= split.length) {
							PCTSEA.logStatus("Skipping line: '" + line + "'", LogLevel.WARN);
							continue;
						}
						final String cellName = removeQuotes(split[cellIDIndex]);
						final String cellType = removeQuotes(
								split[indexesByHeader.get("celltype")].trim().toLowerCase());
						final String tissue = removeQuotes(
								split[indexesByHeader.get("biomaterial")].trim().toLowerCase());

						cellID++;
						singleCellIDsBySingleCellNameMap.put(cellName, cellID);
						cellIDs.add(cellID);
//						final List<edu.scripps.yates.pctsea.db.SingleCell> dbCell = repo.findByName(cellName);
//						if (dbCell.isEmpty()) {
//							cellsNotFound++;
//						}
						final SingleCell singleCell = new SingleCell(cellID, cellName, Double.NaN);
						final CellTypeBranch branch = CellTypeBranch.ORIGINAL;
						singleCell.setCellType(cellType, true, branch);

						// in case of having more columns (not in human cell map)
						if (indexesByHeader.containsKey("developmentstage")) {
							final String developmentStage = split[indexesByHeader.get("developmentstage")];
							singleCell.setDevelopmentStage(developmentStage);
						}
						if (indexesByHeader.containsKey("gender")) {
							final String gender = split[indexesByHeader.get("gender")];
							singleCell.setGender(gender);
						}
						if (indexesByHeader.containsKey("biomaterial")) {
							final String biomaterial = split[indexesByHeader.get("biomaterial")];
							singleCell.setBiomaterial(biomaterial);
						}
						if (indexesByHeader.containsKey("ages")) {
							final String age = split[indexesByHeader.get("ages")];
							singleCell.setAge(age);
						}

						addSingleCell(singleCell);

					}
				}
			} finally {
				reader.close();
				final String message = "Information from " + singleCellList.size() + " single cells read";
				PCTSEA.logStatus(message);
				PCTSEA.logStatus("Cells not found in DB: " + cellsNotFound);
				counter.increment(metadataFile.length());
				final String printIfNecessary = counter.printIfNecessary();
				if (!"".equals(printIfNecessary)) {
					PCTSEA.logStatus(printIfNecessary);
				}
//			System.out.println(message);
			}
		}
		PCTSEA.logStatus(singleCellList.size() + " single cells read");
	}

	private String removeQuotes(String text) {
		if (text.startsWith("\"") || text.endsWith("\"")) {
			return text.replace("\"", "");
		}
		return text;
	}

	public static SingleCell getSingleCellByCellID(int cellID) {
		return singleCellsByCellID.get(cellID);
	}

	public List<SingleCell> getSingleCellListWithCorrelationGT(double minCorrelation) {
		final List<SingleCell> ret = singleCellList.stream().filter(sc -> sc.getScoreForRanking() >= minCorrelation)
				.collect(Collectors.toList());
		PCTSEA.logStatus(ret.size() + " (out of " + singleCellList.size() + ") single cells with correlation >= "
				+ minCorrelation);
		return ret;
	}

	public List<SingleCell> getSingleCellListWithCorrelationLT(double maxCorrelation) {
		final List<SingleCell> ret = singleCellList.stream().filter(sc -> sc.getScoreForRanking() <= maxCorrelation)
				.collect(Collectors.toList());
		PCTSEA.logStatus(ret.size() + " (out of " + singleCellList.size() + ") single cells with correlation <= "
				+ maxCorrelation);
		return ret;
	}

	public void addCorrelations(File correlationsFile) throws IOException {
		if (!correlationsFile.exists()) {
			throw new FileNotFoundException();
		}
		final List<String> lines = Files.readAllLines(correlationsFile.toPath(), Charset.defaultCharset());
		final TObjectIntMap<String> indexesByHeader = new TObjectIntHashMap<String>();
		for (final String line : lines) {
			final String[] split = line.split("\t");
			if (indexesByHeader.isEmpty()) {
				int i = 0;
				for (final String string : split) {
					indexesByHeader.put(string, i++);
				}
			} else {
				final String cellName = split[indexesByHeader.get("SingleCell")];
				final double correlation = Double.valueOf(split[indexesByHeader.get("corr")]);
				if (!singleCellIDsBySingleCellNameMap.containsKey(cellName)) {
					// this means that the single cell was not clustered
					final int cellID = cellIDs.max() + 1;
					final SingleCell singleCell = new SingleCell(cellID, cellName, correlation);
					addSingleCell(singleCell);
				} else {
					singleCellsByCellID.get(singleCellIDsBySingleCellNameMap.get(cellName)).setScore(correlation);
				}
			}
		}
		PCTSEA.logStatus("Now we have " + singleCellList.size() + "(" + singleCellsByCellID.size() + ") single cells");
	}

	/**
	 * NOTE THAT CAN RETURN -1 if the cell is not found because it was ignored from
	 * the db because it didnt have a type
	 * 
	 * @param name
	 * @return
	 */
	public static int getSingleCellIDBySingleCellName(String name) {
		final int id = singleCellIDsBySingleCellNameMap.get(name);
		if (id > 0) {

			return id;
		} else {
			// if it is not found it is because that single cell has not been classified,
			// doesnt have a type and was ignored from the database, therefore, we return
			// -1 here
			return -1;
//			int cellID = 1;
//			if (!cellIDs.isEmpty()) {
//				cellID = cellIDs.max() + 1;
//			}
////			PCTSEA.logStatus("Why cell " + name + " was not found before in the DB?",LogLevel.WARN);
//			final SingleCell cell = new SingleCell(cellID, name, Double.NaN);
//			addSingleCell(cell);
//			return cellID;
		}
	}

	public static void addSingleCell(SingleCell singleCell) {
		addSingleCellIDBySingleCellName(singleCell.getName(), singleCell.getId());
		singleCellList.add(singleCell);
		totalNumCellsForDataset++;
		singleCellsByCellID.put(singleCell.getId(), singleCell);
	}

	private static void addSingleCellIDBySingleCellName(String name, int id) {
		singleCellIDsBySingleCellNameMap.put(name, id);
		cellIDs.add(id);
	}

	public static int getNumSingleCells() {
		return totalNumCellsForDataset;
	}

	public static void clearInformation() {
		singleCellIDsBySingleCellNameMap.clear();
		singleCellList.clear();
		singleCellsByCellID.clear();
	}

}
