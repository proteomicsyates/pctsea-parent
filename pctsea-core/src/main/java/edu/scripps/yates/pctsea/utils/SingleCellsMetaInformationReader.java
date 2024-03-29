package edu.scripps.yates.pctsea.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.model.CellTypeBranch;
import edu.scripps.yates.pctsea.model.SingleCell;
import edu.scripps.yates.pctsea.model.SingleCellSet;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import edu.scripps.yates.utilities.swing.StatusListener;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class SingleCellsMetaInformationReader {
	private static final Logger log = Logger.getLogger(SingleCellsMetaInformationReader.class);

	private StatusListener<Boolean> statusListener;

	private SingleCellSet singleCellSet;

//	/**
//	 * This reads a file with the meta-information of the single cells, their
//	 * classifications, etc...
//	 * 
//	 * @param metadataFile
//	 * @throws IOException
//	 */
//	public SingleCellsMetaInformationReader(File metadataFile) throws IOException {
//		statusListener.onStatusUpdate("Reading single cells metadata file: " + FilenameUtils.getBaseName(metadataFile.getAbsolutePath()));
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
//			statusListener.onStatusUpdate(message);
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
	public SingleCellsMetaInformationReader(File annotationRMBatchFolder, StatusListener<Boolean> statusListener)
			throws IOException {
		singleCellSet = new SingleCellSet();
		this.statusListener = statusListener;
		statusListener.onStatusUpdate(
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
		statusListener.onStatusUpdate(FileUtils.getDescriptiveSizeFromBytes(totalSize) + " from all files");
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
							statusListener.onStatusUpdate(
									"ERROR: File " + metadataFile.getAbsolutePath() + " has different header!");
						}
						final int cellIDIndex = indexesByHeader.get("cell_id");
						if (cellIDIndex >= split.length) {
							statusListener.onStatusUpdate("WARN: Skipping line: '" + line + "'");
							continue;
						}
						final String cellName = removeQuotes(split[cellIDIndex]);
						final String cellType = removeQuotes(
								split[indexesByHeader.get("celltype")].trim().toLowerCase());
						final String tissue = removeQuotes(
								split[indexesByHeader.get("biomaterial")].trim().toLowerCase());

						cellID++;
						singleCellSet.getSingleCellIDsBySingleCellNameMap().put(cellName, cellID);
						singleCellSet.getCellIDs().add(cellID);
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

						singleCellSet.addSingleCell(singleCell);

					}
				}
			} finally {
				reader.close();
				final String message = "Information from " + singleCellSet.getSingleCellList().size()
						+ " single cells read";
				statusListener.onStatusUpdate(message);
				statusListener.onStatusUpdate("Cells not found in DB: " + cellsNotFound);
				counter.increment(metadataFile.length());
				final String printIfNecessary = counter.printIfNecessary();
				if (!"".equals(printIfNecessary)) {
					statusListener.onStatusUpdate(printIfNecessary);
				}
//			System.out.println(message);
			}
		}
		statusListener.onStatusUpdate(singleCellSet.getSingleCellList().size() + " single cells read");
	}

	private String removeQuotes(String text) {
		if (text.startsWith("\"") || text.endsWith("\"")) {
			return text.replace("\"", "");
		}
		return text;
	}

	public List<SingleCell> getSingleCellListWithCorrelationGT(double minCorrelation) {
		final List<SingleCell> ret = singleCellSet.getSingleCellList().stream()
				.filter(sc -> sc.getScoreForRanking() >= minCorrelation).collect(Collectors.toList());
		statusListener.onStatusUpdate(ret.size() + " (out of " + singleCellSet.getSingleCellList().size()
				+ ") single cells with correlation >= " + minCorrelation);
		return ret;
	}

	public List<SingleCell> getSingleCellListWithCorrelationLT(double maxCorrelation) {
		final List<SingleCell> ret = singleCellSet.getSingleCellList().stream()
				.filter(sc -> sc.getScoreForRanking() <= maxCorrelation).collect(Collectors.toList());
		statusListener.onStatusUpdate(ret.size() + " (out of " + singleCellSet.getSingleCellList().size()
				+ ") single cells with correlation <= " + maxCorrelation);
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
				if (!singleCellSet.getSingleCellIDsBySingleCellNameMap().containsKey(cellName)) {
					// this means that the single cell was not clustered
					final int cellID = singleCellSet.getCellIDs().max() + 1;
					final SingleCell singleCell = new SingleCell(cellID, cellName, correlation);
					singleCellSet.addSingleCell(singleCell);
				} else {
					singleCellSet.getSingleCellsByCellID()
							.get(singleCellSet.getSingleCellIDsBySingleCellNameMap().get(cellName))
							.setScore(correlation);
				}
			}
		}
		statusListener.onStatusUpdate("Now we have " + singleCellSet.getSingleCellList().size() + "("
				+ singleCellSet.getSingleCellsByCellID().size() + ") single cells");
	}

}
