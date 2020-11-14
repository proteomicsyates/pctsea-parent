package edu.scripps.yates.pctsea.db.datasets.singlecellshuman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.SingleCell;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.files.FileUtils;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class HumanSingleCellsDatasetCreation {
	private final static Logger log = Logger.getLogger(HumanSingleCellsDatasetCreation.class);

	private final File singleCellExpressionFolder;
	private int BATCH_SIZE = 1000;
	private final DatasetMongoRepository projectMongoRepo;
	private final SingleCellMongoRepository singleCellMongoRepository;
	private final MongoBaseService mongoBaseService;

	private Set<String> singleCellsInDB;
	private static long id = 0;

	public HumanSingleCellsDatasetCreation(File singleCellExpressionFolder, File annotationRMBatchFolder,
			DatasetMongoRepository projectMongoRepo, SingleCellMongoRepository singleCellMongoRepository,
			MongoBaseService mongoBaseService, Integer batchSize) throws IOException {
		// read single cells
		new SingleCellsMetaInformationReader(annotationRMBatchFolder);
		this.singleCellExpressionFolder = singleCellExpressionFolder;
		this.projectMongoRepo = projectMongoRepo;
		this.singleCellMongoRepository = singleCellMongoRepository;
		this.mongoBaseService = mongoBaseService;
		if (batchSize != null) {
			this.BATCH_SIZE = batchSize;
		}
	}

	public void run() throws IOException {
		final long t0 = System.currentTimeMillis();
		final Dataset project = new Dataset();
		project.setTag("HCL");
		project.setName("Construction of a human cell landscape at single-cell level");
		project.setReference("https://doi.org/10.1038/s41586-020-2157-4");
		if (projectMongoRepo.findByName(project.getName()).isEmpty()) {
			projectMongoRepo.save(project);
		}

		readExpressionValuesByInteractorGenes(project);
		System.out.println("Everything ok!!");
		final long t1 = System.currentTimeMillis() - t0;
		System.out.println("It took " + DatesUtil.getDescriptiveTimeFromMillisecs(t1));
	}

	private void readExpressionValuesByInteractorGenes(Dataset project) throws IOException {

		final File[] files = singleCellExpressionFolder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (FilenameUtils.getExtension(name).equals("gz")) {
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
		System.out.println(FileUtils.getDescriptiveSizeFromBytes(totalSize) + " from all files");
		final ProgressCounter counter = new ProgressCounter(totalSize, ProgressPrintingType.EVERY_STEP, 1, true);
		for (int numFile = 1; numFile <= files.length; numFile++) {

			final File file = files[numFile - 1];
			System.out.println("Reading  file " + FilenameUtils.getName(file.getAbsolutePath()) + " (" + numFile + "/"
					+ files.length + ")");
			final List<Expression> sces = readSingleCellGZipFileAndSaveSingleCells(file, project);
			System.out.println("Saving " + sces.size() + " single cell expressions...");
			final long t0 = System.currentTimeMillis();
			final List<Expression> batch = new ArrayList<Expression>();

			for (final Expression sce : sces) {
				batch.add(sce);
				if (batch.size() == BATCH_SIZE) {
					mongoBaseService.saveExpressions(batch);
					final long t1 = System.currentTimeMillis() - t0;
//					System.out.println(batch.size() + " entities saved in database in "
//							+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " " + num + "/" + sces.size());

					batch.clear();
				}
			}
			if (!batch.isEmpty()) {
				mongoBaseService.saveExpressions(batch);

			}
			counter.increment(file.length());
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				log.info(printIfNecessary);
			}
			final long t1 = System.currentTimeMillis() - t0;
			System.out.println(
					sces.size() + " entities saved in database in " + DatesUtil.getDescriptiveTimeFromMillisecs(t1)
							+ ", avg:" + DatesUtil.getDescriptiveTimeFromMillisecs((1.0 * t1 / sces.size()))
							+ " per expression with batch size " + BATCH_SIZE);
		}

	}

	private List<Expression> readSingleCellGZipFileAndSaveSingleCells(File file, Dataset dataset) throws IOException {

		if (singleCellsInDB == null) {

			singleCellsInDB = this.singleCellMongoRepository.findAll().stream().map(sc -> sc.getName())
					.collect(Collectors.toSet());
			log.info(singleCellsInDB.size() + " single cells in DB " + this.singleCellMongoRepository.count());
		}
		// final THashMap<String, TObjectIntMap<String>> expressionsByCell = new
		// THashMap<String, TObjectIntMap<String>>();
		BufferedReader br = null;
		final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
		final Map<String, SingleCell> singleCellByNames = new THashMap<String, SingleCell>();
		final List<Expression> sces = new ArrayList<Expression>();
		try {

			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			String line = br.readLine();
//			final TObjectIntMap<String> indexByHeader = new TObjectIntHashMap<String>();
			String[] headers = null;
			int[] indexes = null;
			int numLine = 1;

			while (line != null) {
				try {
					if (line.startsWith("#")) {
						continue;
					}

//					final List<String> split = Splitter.on(',').splitToList(line); // this is slower
					final String[] split = line.split(",");
					if (headers == null) {
						headers = new String[split.length];
						indexes = new int[split.length];
						for (int index = 0; index < split.length; index++) {
							final String string = split[index].replace("\"", "");
							headers[index] = string;
							indexes[index] = index + 1;
//							indexByHeader.put(string, index + 1); // header is shifted one position to the left
						}

						continue;
					}
					final String gene = split[0].replace("\"", "").toUpperCase();
//					for (final String header : indexByHeader.keySet()) {

					for (int i = 0; i < headers.length; i++) {
						final String header = headers[i];
						final int index = indexes[i];
						if (!header.equals("")) {
							final String expressionValueString = split[index];
							final short expressionValue = Short.valueOf(expressionValueString);
							if (expressionValue > 0) {
								final String singleCellName = header;
								final String type = getSingleCellType(singleCellName);
								final String biomaterial = getSingleCellBiomaterial(singleCellName);
								SingleCell singleCelldb = null;
								if (!singleCellByNames.containsKey(singleCellName)) {
									singleCelldb = new SingleCell(singleCellName, type, biomaterial, dataset.getTag());
									singleCellList.add(singleCelldb);
									singleCellByNames.put(singleCellName, singleCelldb);
								} else {
									singleCelldb = singleCellByNames.get(singleCellName);
								}

								final Expression sce = new Expression();

								sce.setCell(singleCelldb);
								sce.setGene(gene);
								sce.setExpression(expressionValue);
								sce.setProjectTag(dataset.getTag());
								sces.add(sce);
//								if (sces.size() >= 100) {
//									break;
//								}
//								matrix.addValue(singleCell, gene, expressionValue);
							} else if (expressionValue < 0) {
								throw new IllegalArgumentException(
										"Maybe we need something else than a short for this: " + expressionValueString);
							}
						}
					}

				} finally {
					line = br.readLine();
					numLine++;
//					if (sces.size() >= 100) {
//						break;
//					}
				}
			}

		} finally {
			br.close();
		}

		//
		final List<SingleCell> batch = new ArrayList<SingleCell>();
		final long t0 = System.currentTimeMillis();
		for (final SingleCell sc : singleCellList) {
			if (!singleCellsInDB.contains(sc.getName())) {
				batch.add(sc);
			}
			if (batch.size() == BATCH_SIZE) {

				mongoBaseService.saveSingleCells(batch);
				singleCellsInDB.addAll(batch.stream().map(sc2 -> sc2.getName()).collect(Collectors.toList()));
				batch.clear();
			}
		}
		if (!batch.isEmpty()) {
			mongoBaseService.saveSingleCells(batch);

			singleCellsInDB.addAll(batch.stream().map(sc2 -> sc2.getName()).collect(Collectors.toList()));
		}
		final long t1 = System.currentTimeMillis() - t0;
		System.out.println(singleCellList.size() + " single cells saved in database in "
				+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " in last batch");
		return sces;

	}

	private String getSingleCellBiomaterial(String singleCell) {
		final int cellID = SingleCellsMetaInformationReader.getSingleCellIDBySingleCellName(singleCell);
		final edu.scripps.yates.pctsea.model.SingleCell cell = SingleCellsMetaInformationReader
				.getSingleCellByCellID(cellID);
		if (cell != null) {
			return cell.getBiomaterial();
		}
		return null;
	}

	private String getSingleCellType(String singleCell) {
		final int cellID = SingleCellsMetaInformationReader.getSingleCellIDBySingleCellName(singleCell);
		final edu.scripps.yates.pctsea.model.SingleCell cell = SingleCellsMetaInformationReader
				.getSingleCellByCellID(cellID);
		if (cell != null) {
			return cell.getOriginalCellType();
		}
		return null;
	}

	protected static Set<String> readGenesFromSingleCellGZipFile(File file) throws IOException {
		final Set<String> ret = new THashSet<String>();
		BufferedReader br = null;
		try {
			log.info("Reading gzip file " + file.getAbsolutePath());
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			String line = br.readLine();
			int numLine = 1;
			while (line != null) {

				try {
					if (numLine == 1) {
						continue;
					}
					if (line.startsWith("#")) {
						continue;
					}

					final String gene = line.substring(0, line.indexOf(",")).replace("\"", "");
					ret.add(gene);
				} finally {
					line = br.readLine();
					numLine++;
				}
			}
			log.info(ret.size() + " genes found in " + FilenameUtils.getName(file.getAbsolutePath()));
		} finally {
			br.close();

		}
		return ret;
	}
}
