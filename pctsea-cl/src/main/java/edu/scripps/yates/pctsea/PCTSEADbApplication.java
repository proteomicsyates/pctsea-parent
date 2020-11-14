package edu.scripps.yates.pctsea;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import edu.scripps.yates.pctsea.db.datasets.singlecellshuman.GeneToUpperCase;
import edu.scripps.yates.pctsea.db.datasets.singlecellshuman.HumanSingleCellsDatasetCreation;
import edu.scripps.yates.utilities.swing.DoNotInvokeRunMethod;

@SpringBootApplication
public class PCTSEADbApplication implements CommandLineRunner {
	private final static boolean FORCE_DB_LOCAL = true;
	@Autowired
	private DatasetMongoRepository pmr;
	@Autowired
	private SingleCellMongoRepository scmr;
	@Autowired
	private MongoBaseService mbs;

	@Autowired
	private ExpressionMongoRepository emr;

	public static void main(String[] args) {

		// if we find the envirnoment variable MONGO_TUNNEL_PORT, it will use the
		// application-remoteTunnel.properties which uses a different db port
		final String mongoTunnelPort = "MONGO_TUNNEL_PORT";
		boolean useTunnelProperties = false;

		final Map<String, String> getenv = System.getenv();
		final String port = getenv.get(mongoTunnelPort);
		if (port != null) {
			System.out.println(mongoTunnelPort + ":" + port);
			if (!FORCE_DB_LOCAL && port.equals("27017")) {
				useTunnelProperties = true;

			}
		}

		final SpringApplicationBuilder builder = new SpringApplicationBuilder(PCTSEADbApplication.class);
		builder.headless(false);
		if (useTunnelProperties) {
			System.out.println(
					"SSH tunnel configuration detected. Using application-remoteTunnel.properties files to connect to remote MongoDB");
			builder.properties("spring.config.location=classpath:/application-remoteTunnel.properties")
					.logStartupInfo(true);
		} else {
			builder.logStartupInfo(false);
		}
		final ConfigurableApplicationContext run = builder.build().run(args);

	}

	private static Properties getPropertiesForTunnel() throws IOException {
		final Properties properties = new Properties();
		properties.load(
				PCTSEADbApplication.class.getClassLoader().getResourceAsStream("application-remoteTunnel.properties"));
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
			c = new PCTSEACommandLine(args, pmr, emr, scmr, mbs);

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
