package edu.scripps.yates.pctsea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import edu.scripps.yates.pctsea.db.Dataset;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.SingleCell;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.utils.SingleCellsMetaInformationReader;
import edu.scripps.yates.utilities.dates.DatesUtil;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

/**
 * This class serves as Spring application to save single cell expressions from
 * study:<br>
 * "A single cell atlas of the airway epithelium reveals the CFTR-rich pulmonary
 * ionocyte"<br>
 * https://doi.org/10.1038/s41586-018-0394-6<br>
 * In order to run this, un-comment the annotation @SpringBootApplication and
 * create the jar file with profile 'epithelium'
 * 
 * @author salvador
 *
 */
//@SpringBootApplication
public class EpitheliumCells implements CommandLineRunner {
	private final static Logger log = Logger.getLogger(EpitheliumCells.class);

	private File singleCellExpressionTableFile;
	private File singleCellMetadataFile;
	private final int BATCH_SIZE = 10000;
	@Autowired
	private DatasetMongoRepository datasetsMongoRepo;
	@Autowired
	private MongoBaseService mongoBaseService;
	@Autowired
	private SingleCellMongoRepository singleCellMongoRepo;
	@Autowired
	private ExpressionMongoRepository expressionMongoRepo;

	private Dataset dataset;

	private String biomaterial;

	public static void main(String[] args) {
		final SpringApplicationBuilder builder = new SpringApplicationBuilder(EpitheliumCells.class);
		builder.headless(false).logStartupInfo(false);

		final ConfigurableApplicationContext run = builder.build().run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		singleCellMetadataFile = new File(args[0]);
		singleCellExpressionTableFile = new File(args[1]);

		final long t0 = System.currentTimeMillis();
		dataset = new Dataset();
		dataset.setTag("GSE102580");
		dataset.setName("Single cell atlas of the airway epithelium");
		dataset.setReference("https://doi.org/10.1038/s41586-018-0394-6");
		final List<Dataset> datasetsDB = datasetsMongoRepo.findByTag(dataset.getTag());
		if (datasetsDB.isEmpty()) {
			datasetsMongoRepo.save(dataset);
		} else {
			dataset = datasetsDB.get(0);
		}

		readExpressionValuesByInteractorGenes(dataset);
		System.out.println("Everything ok!!");
		final long t1 = System.currentTimeMillis() - t0;
		System.out.println("It took " + DatesUtil.getDescriptiveTimeFromMillisecs(t1));
	}

	private void readExpressionValuesByInteractorGenes(Dataset dataset) throws IOException {

		// read metadata
		final Map<String, SingleCell> cellsByCellID = readCells(singleCellMetadataFile, dataset);
		saveSingleCells(cellsByCellID.values());
		final List<Expression> sces = readExpressions(singleCellExpressionTableFile, cellsByCellID, dataset);

		saveExpressions(sces);

	}

	private void saveSingleCells(Collection<SingleCell> cells) {
		final ProgressCounter counter = new ProgressCounter(cells.size(), ProgressPrintingType.PERCENTAGE_STEPS, 1,
				true);
		System.out.println("Saving " + cells.size() + " single cells...");
		final List<SingleCell> batch = new ArrayList<SingleCell>();
		int num = 0;
		long t0 = 0l;
		long t1 = 0l;
		for (final SingleCell cell : cells) {
			cell.setId(null);// in order to be saved
			counter.increment();
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				log.info(printIfNecessary);
			}
			final List<SingleCell> findByName = singleCellMongoRepo.findByName(cell.getName());
			if (findByName.isEmpty()) {
				batch.add(cell);
			} else {
				if (findByName.size() == 1) {
					cell.setId(findByName.get(0).getId());
				} else {
					// delete all but first
					for (int i = 1; i < findByName.size(); i++) {
						singleCellMongoRepo.delete(findByName.get(i));
					}
					cell.setId(findByName.get(0).getId());
				}
			}
			if (batch.size() == BATCH_SIZE) {
				t0 = System.currentTimeMillis();
				num += batch.size();
				mongoBaseService.saveSingleCells(batch);
				t1 = System.currentTimeMillis() - t0;
				System.out.println(batch.size() + " entities saved in database in "
						+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " " + num + "/" + cells.size());

				batch.clear();

				System.out.println(batch.size() + " entities saved in database in "
						+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " in last batch");
			}
		}
		t0 = System.currentTimeMillis();
		if (!batch.isEmpty()) {
			mongoBaseService.saveSingleCells(batch);
			t1 = System.currentTimeMillis() - t0;
			System.out.println(batch.size() + " entities saved in database in "
					+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " " + num + "/" + cells.size());
		}
	}

	private void saveExpressions(List<Expression> sces) {
		final ProgressCounter counter = new ProgressCounter(sces.size(), ProgressPrintingType.PERCENTAGE_STEPS, 1,
				true);
		System.out.println("Saving " + sces.size() + " single cell expressions...");
		final List<Expression> batch = new ArrayList<Expression>();
		final Map<String, List<Expression>> expressionsByCellName = new THashMap<String, List<Expression>>();
		int num = 0;
		long t0 = 0l;
		long t1 = 0l;
		for (final Expression sce : sces) {
			counter.increment();
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				log.info(printIfNecessary);
			}
			List<Expression> expressionsFromDB = null;
			if (!expressionsByCellName.containsKey(sce.getCellName())) {
				expressionsFromDB = expressionMongoRepo.findExpressionsByCellName(sce.getCellName());
				expressionsByCellName.put(sce.getGene(), expressionsFromDB);
			} else {
				expressionsFromDB = expressionsByCellName.get(sce.getCellName());
			}
			final List<Expression> found = new ArrayList<Expression>();
			for (final Expression expression : expressionsFromDB) {
				if (expression.getGene().equals(sce.getGene())) {
					if (expression.getProjectTag().equals(sce.getProjectTag())) {
						if (Float.compare(expression.getExpression(), sce.getExpression()) == 0) {
							if (found.isEmpty()) {
								found.add(expression);
							} else {
								expressionMongoRepo.delete(expression);
							}
						}
					}
				}
			}
			if (found.isEmpty()) {
				batch.add(sce);
			}
			if (batch.size() == BATCH_SIZE) {
				t0 = System.currentTimeMillis();
				num += batch.size();
				mongoBaseService.saveExpressions(batch);
				t1 = System.currentTimeMillis() - t0;

//				System.out.println(batch.size() + " entities saved in database in "
//						+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " in last batch");
				batch.clear();
			}
		}
		t0 = System.currentTimeMillis();

		mongoBaseService.saveExpressions(batch);
		t1 = System.currentTimeMillis() - t0;
//		System.out.println(batch.size() + " entities saved in database in "
//				+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " " + num + "/" + sces.size());
	}

	private List<Expression> readExpressions(File singleCellExpressionsFile,
			Map<String, SingleCell> singleCellsByCellID, Dataset project) throws IOException {
		log.info("Reading expressions of genes on cells");
		final List<Expression> ret = new ArrayList<Expression>();
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(this.singleCellExpressionTableFile));
			String line;
			TObjectIntMap<String> indexesByColumnName = null;
			List<String> cellIDs = null;
			while ((line = r.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				if (indexesByColumnName == null) {
					indexesByColumnName = getIndexesByColumnName(line);
					cellIDs = indexesByColumnName.keySet().stream().collect(Collectors.toList());
				} else {
					final String[] split = line.split("\t");
					final String geneName = split[0].trim();
					for (final String cellID : cellIDs) {
						if ("".equals(cellID)) {
							continue;
						}
						final int index = indexesByColumnName.get(cellID);

						final float expression = Float.valueOf(split[index]);
						// only the ones that are not zero!
						if (Float.compare(expression, 0f) != 0) {
							final SingleCell cell = singleCellsByCellID.get(cellID);
							final Expression expressionValue = new Expression(cell, geneName, expression,
									project.getTag());
							ret.add(expressionValue);
						}
					}
				}
			}
		} finally {
			if (r != null) {
				r.close();
			}
			log.info(ret.size() + " expressions of genes on cells read");
		}
		return ret;
	}

	private Map<String, SingleCell> readCells(File singleCellMetadataFile2, Dataset dataset) throws IOException {
		final Map<String, SingleCell> ret = new THashMap<String, SingleCell>();
		final List<String> lines = Files.readAllLines(singleCellMetadataFile.toPath());
		TObjectIntMap<String> indexesByColumnName = null;
		final Set<String> cellTypes = new THashSet<String>();// just to log the number
		for (final String line : lines) {
			// skip lines starting by #
			if (line.startsWith("#")) {
				continue;
			}
			if (indexesByColumnName == null) {
				indexesByColumnName = getIndexesByColumnName(line);
				continue;
			} else {
				final String[] split = line.split("\t");
				final int cellIndex = Integer.valueOf(split[indexesByColumnName.get("")].trim());
				final String cellBarCode = split[indexesByColumnName.get("barcode")].trim();
				final String cellClassification = split[indexesByColumnName.get("clusters_Fig1")].trim();
				final SingleCell singleCell = new SingleCell(cellBarCode, cellClassification, biomaterial,
						dataset.getTag());
				cellTypes.add(singleCell.getType());
				singleCell.setId(String.valueOf(cellIndex));
				ret.put(singleCell.getId(), singleCell);
			}
		}
		log.info(ret.size() + " cells from " + cellTypes.size() + " classifications read from metadata file");
		return ret;
	}

	private TObjectIntMap<String> getIndexesByColumnName(String line) {
		final TObjectIntMap<String> ret = new TObjectIntHashMap<String>();
		final String[] split = line.split("\t");
		for (int i = 0; i < split.length; i++) {
			ret.put(split[i], i);
		}
		return ret;
	}

	private List<Expression> readSingleCellGZipFile(File file, Dataset dataset) throws IOException {
//		final THashMap<String, TObjectIntMap<String>> expressionsByCell = new THashMap<String, TObjectIntMap<String>>();
		BufferedReader br = null;
		final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
		final Map<String, SingleCell> singleCellByNames = new THashMap<String, SingleCell>();
		final List<Expression> sces = new ArrayList<Expression>();
		try {

			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			String line = br.readLine();
			final TObjectIntMap<String> indexByHeader = new TObjectIntHashMap<String>();
			int numLine = 1;

			while (line != null) {
				try {
					if (line.startsWith("#")) {
						continue;
					}

//					final List<String> split = Splitter.on(',').splitToList(line); // this is slower
					final String[] split = line.split(",");
					if (indexByHeader.isEmpty()) {
						for (int index = 0; index < split.length; index++) {
							final String string = split[index].replace("\"", "");
							indexByHeader.put(string, index + 1); // header is shifted one position to the left
						}
						continue;
					}
					final String gene = split[0].replace("\"", "");
					for (final String header : indexByHeader.keySet()) {
						if (!header.equals("")) {
							final String expressionValueString = split[indexByHeader.get(header)];
							final short expressionValue = Short.valueOf(expressionValueString);
							if (expressionValue > 0) {
								final String singleCellName = header;
								final String type = getSingleCellType(singleCellName);

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
		int num = 0;

		for (final SingleCell sc : singleCellList) {
			batch.add(sc);
			if (batch.size() == BATCH_SIZE) {
				final long t0 = System.currentTimeMillis();
				num += batch.size();
				mongoBaseService.saveSingleCells(batch);
				final long t1 = System.currentTimeMillis() - t0;
				System.out.println(batch.size() + " single cells saved in database in "
						+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " " + num + "/" + singleCellList.size());

				batch.clear();
			}
		}
		final long t0 = System.currentTimeMillis();
		mongoBaseService.saveSingleCells(batch);
		final long t1 = System.currentTimeMillis() - t0;
		System.out.println(batch.size() + " single cells saved in database in "
				+ DatesUtil.getDescriptiveTimeFromMillisecs(t1) + " in last batch");
		return sces;

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
