package edu.scripps.yates.pctsea;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;

import com.mongodb.MongoTimeoutException;

import edu.scripps.yates.pctsea.db.CellTypeAndGeneMongoRepository;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.PctseaRunLogRepository;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.db.datasets.singlecellshuman.GeneToUpperCase;
import edu.scripps.yates.pctsea.db.datasets.singlecellshuman.HumanSingleCellsDatasetCreation;
import edu.scripps.yates.utilities.strings.StringUtils;
import edu.scripps.yates.utilities.swing.DoNotInvokeRunMethod;

@SpringBootApplication
public class PCTSEADbApplication implements CommandLineRunner {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(PCTSEADbApplication.class);
	@Autowired
	private DatasetMongoRepository pmr;
	@Autowired
	private SingleCellMongoRepository scmr;
	@Autowired
	private MongoBaseService mbs;
	@Autowired
	private PctseaRunLogRepository runLogsRepo;
	@Autowired
	private CellTypeAndGeneMongoRepository ctgmr;
	@Autowired
	private ExpressionMongoRepository emr;

	/**
	 * Use --spring.data.mongodb.host=myhost.domain.com and
	 * spring.data.mongodb.port=88888 to override the default DB connection to
	 * MongoDB that is in localhost and port 27017
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Starting app with input params: " + getStringFromParams(args));

		/////////////////////////////////////////////////////

		final SpringApplicationBuilder builder = new SpringApplicationBuilder(PCTSEADbApplication.class);
		builder.headless(false);

//		System.out.println("Using spring.data.mongodb.host:" + pctseaMongoDBServerHost);
//		System.out.println("Using spring.data.mongodb.port:" + pctseaMongoHost);
//		builder.properties("spring.data.mongodb.host:" + pctseaMongoDBServerHost)// + ",spring.data.mongodb.port:" +
//																					// port)
		log.info("Building Spring application...");
		final SpringApplication springApp = builder.logStartupInfo(true).build();
		log.info("Spring application built");
		// remove spring params to not interfere with the pctsea params
//		args = removeSpringParams(args);
		try {
			final ConfigurableApplicationContext context = springApp.run(args);
		} catch (final Exception e) {
			if (e instanceof DataAccessResourceFailureException) {
				if (e.getCause() != null && e.getCause() instanceof MongoTimeoutException) {
					log.error("Some error occurred trying to connect to the database: " + e.getCause().getMessage());
				}
			}
			System.exit(-1);
		}

	}

	private static String getStringFromParams(String[] args) {
		return StringUtils.getSeparatedValueStringFromChars(args, ",");
	}

	private static String[] removeSpringParams(String[] args) {
		final List<String> ret = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--spring.")) {
				i++; // skip this and the next
				continue;
			} else {
				ret.add(args[i]);
			}
		}
		return ret.toArray(new String[0]);
	}

	private static String getParam(String[] args, String param) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-" + param)) {
				if (i + 1 < args.length) {
					return args[i + 1];
				}
			}
		}
		return null;
	}

	private static boolean containsParam(String[] args, String param) {
		return getParam(args, param) != null;
	}

	private static Properties getPropertiesForTunnel() throws IOException {
		final Properties properties = new Properties();
		properties.load(
				PCTSEADbApplication.class.getClassLoader().getResourceAsStream("application-remoteTunnel.properties"));
		return properties;
	}

	private static Properties getPropertiesForConnectionToSealion() throws IOException {
		final Properties properties = new Properties();
		properties.load(PCTSEADbApplication.class.getClassLoader()
				.getResourceAsStream("application-connection-to-sealion.properties"));
		return properties;
	}

	@Override
	public void run(String... args) throws Exception {

//		readSingleCellGZipFile(new File(
//				"C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\dge_rmbatch_data\\Adult-Muscle1_rmbatchdge.txt.gz"),
//				new Project());
//		if (true) {
//			return;
//		}

		// // this code was used to insert data
//		if (args.length == 3 || args.length == 2) {
//			System.err.println(
//					"creating database with 3 arguments: folder with cells info, metadata of cells file and batch file");
//
//			Integer batchSize = null;
//			if (args.length == 3) {
//				batchSize = Integer.valueOf(args[2]);
//			}
//
//			final File singleCellExpressionFolder = new File(args[0]);
//			final File singleCellMetadata = new File(args[1]);
//			final SingleCellsMetaInformationReader reader = new SingleCellsMetaInformationReader(singleCellMetadata);
//			reader.getSingleCellList();
//			final DataExtractionFromHumanSingleCells tableGenerator = new DataExtractionFromHumanSingleCells(
//					singleCellExpressionFolder, pmr, scr, mongoBaseService, batchSize);
//			tableGenerator.run();
//			return;
////
//		}

		if (args.length == 1) {
			if (args[0].equals("test")) {
				return;
			} else if (args[0].equals("gene_to_uppercase")) {
				final GeneToUpperCase geneToUpperCase = new GeneToUpperCase(mbs, emr);
				geneToUpperCase.run();
				return;
			}
		} else if (args.length > 0 && args[0].equals("HCL")) {
			final File expressionFolder = new File(args[1]);
			final File metadataFolder = new File(args[2]);
			int batchSize = 5000;
			if (args.length == 4) {
				batchSize = Integer.valueOf(args[3]);
			}
			System.out.println("Saving HCL dataset with:");
			System.out.println(expressionFolder.getAbsolutePath());
			System.out.println(metadataFolder.getAbsolutePath());
			System.out.println("BATCH_SIZE:" + batchSize);

			final HumanSingleCellsDatasetCreation hsc = new HumanSingleCellsDatasetCreation(expressionFolder,
					metadataFolder, pmr, scmr, mbs, batchSize);
			hsc.run();
			return;
		}

		PCTSEACommandLine c = null;
		try {
			args = removeSpringParams(args);
			c = new PCTSEACommandLine(args, pmr, emr, scmr, runLogsRepo, ctgmr, mbs);

			c.safeRun();

		} catch (final DoNotInvokeRunMethod e) {
			// do nothing
		} catch (final Exception e) {
			e.printStackTrace();
			if (c != null) {
				if (!c.isUsingGUI()) {
					System.exit(-1);
				}
			} else {
				System.exit(-1);
			}
		}
	}

//	private List<Expression> readSingleCellGZipFile(File file, Project project) throws IOException {
////		final THashMap<String, TObjectIntMap<String>> expressionsByCell = new THashMap<String, TObjectIntMap<String>>();
//		BufferedReader br = null;
//		final List<SingleCell> singleCellList = new ArrayList<SingleCell>();
//		final Map<String, SingleCell> singleCellByNames = new THashMap<String, SingleCell>();
//		final List<Expression> sces = new ArrayList<Expression>();
//		try {
//
//			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
//			String line = br.readLine();
//			final TObjectIntMap<String> indexByHeader = new TObjectIntHashMap<String>();
//			int numLine = 1;
//
//			while (line != null) {
//				try {
//					if (line.startsWith("#")) {
//						continue;
//					}
//
////					final List<String> split = Splitter.on(',').splitToList(line); // this is slower
//					final String[] split = line.split(",");
//					if (indexByHeader.isEmpty()) {
//						for (int index = 0; index < split.length; index++) {
//							final String string = split[index].replace("\"", "");
//							indexByHeader.put(string, index + 1); // header is shifted one position to the left
//						}
//						continue;
//					}
//					final String gene = split[0].replace("\"", "");
//					for (final String header : indexByHeader.keySet()) {
//						if (!header.equals("")) {
//							final String expressionValueString = split[indexByHeader.get(header)];
//							final short expressionValue = Short.valueOf(expressionValueString);
//							if (expressionValue > 0) {
//								final String singleCellName = header;
//								if (singleCellName.equals("AdultMuscle_1.CCAGACAATAAATCGTAA")
//										&& gene.equalsIgnoreCase("TPM2")) {
//									System.out.println("asdf");
//								}
//								final String type = getSingleCellType(singleCellName);
//
//								SingleCell singleCelldb = null;
//								if (!singleCellByNames.containsKey(singleCellName)) {
//									singleCelldb = new SingleCell(singleCellName, type);
//									singleCellList.add(singleCelldb);
//									singleCellByNames.put(singleCellName, singleCelldb);
//								} else {
//									singleCelldb = singleCellByNames.get(singleCellName);
//								}
//
//								final Expression sce = new Expression();
//
//								sce.setCell(singleCelldb);
//								sce.setGene(gene);
//								sce.setExpression(expressionValue);
//								sce.setProject(project);
//								sces.add(sce);
////								if (sces.size() >= 100) {
////									break;
////								}
////								matrix.addValue(singleCell, gene, expressionValue);
//							} else if (expressionValue < 0) {
//								throw new IllegalArgumentException(
//										"Maybe we need something else than a short for this: " + expressionValueString);
//							}
//						}
//					}
//
//				} finally {
//					line = br.readLine();
//					numLine++;
////					if (sces.size() >= 100) {
////						break;
////					}
//				}
//			}
//
//		} finally {
//			br.close();
//		}
//
//		//
//
//		return sces;
//
//	}
//
//	private String getSingleCellType(String singleCell) {
//		final int cellID = SingleCellsMetaInformationReader.getSingleCellIDBySingleCellName(singleCell);
//		final edu.scripps.yates.pctsea.SingleCell cell = SingleCellsMetaInformationReader.getSingleCellByCellID(cellID);
//		if (cell != null) {
//			return cell.getOriginalCellType();
//		}
//		return null;
//	}
}
