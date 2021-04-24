package edu.scripps.yates.pctsea;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import edu.scripps.yates.pctsea.db.CellTypeAndGeneMongoRepository;
import edu.scripps.yates.pctsea.db.CellTypeAndGenesDBUtil;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

@RunWith(SpringRunner.class)
@AutoConfigureDataMongo
@SpringBootTest(//
// using args="test" makes that PCTSEADbApplication don't start as usually (see
// in its code)
		args = { "test",
//		"HCL",
//				"C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\data_from_article\\dge_rmbatch_data",
//				"C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\data_from_article\\annotation_rmbatch_data_revised417",
//				"10000"
		}, //
			// we don't want a web environment to test
		webEnvironment = WebEnvironment.NONE, //
		properties = { "headles=false" //
		// to force to conect to the remote DB with tunnel
				, "spring.config.location=classpath:/application-remoteTunnel.properties"//
//				, "spring.jpa.hibernate.ddl-auto=create" 
		})
public class GeneExpressionExplorer {
	@Autowired
	DatasetMongoRepository projectMongoRepo;
	@Autowired
	SingleCellMongoRepository singleCellMongoRepository;
	@Autowired
	ExpressionMongoRepository expressionMongoRepository;
	@Autowired
	MongoBaseService mongoBaseService;
	@Autowired
	CellTypeAndGeneMongoRepository cellTypeGeneRepo;

	@Test
	public void testGeneExpressionExplorerTest() {
		final String gene = "GRIA1";
		final int num_cells_total = (int) singleCellMongoRepository.count();
		final List<Expression> expressions = expressionMongoRepository.findByGene(gene);
		final int num_cells_gene_total = expressions.size();
		System.out.println(gene + " is expressed in " + expressions.size() + " cells");
		final Map<String, TFloatList> expressionsByCellType = new THashMap<String, TFloatList>();
		for (final Expression expression : expressions) {
			final String cellType = expression.getCellType();
			if (cellType == null) {
				continue;
			}
			if (!expressionsByCellType.containsKey(cellType)) {
				expressionsByCellType.put(cellType, new TFloatArrayList());
			}
			expressionsByCellType.get(cellType).add(expression.getExpression());
		}
		try {
			final FileWriter fw = new FileWriter(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_12-42-56_SIMPLE_SCORE_NEGSUP_TYPE\\gene_distributions.txt");
			final FileWriter fw2 = new FileWriter(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_12-42-56_SIMPLE_SCORE_NEGSUP_TYPE\\cell_type_hyperG_for_gene.txt");
			fw2.write(
					"gene\tcellType\tp-value\ttotal num cells\tcells with gene\tcells of type\tcells with gene of type\n");
			final List<String> cellTypes = expressionsByCellType.keySet().stream().distinct().sorted()
					.collect(Collectors.toList());
			for (final String cellType : cellTypes) {
				final int num_cells_type_total = singleCellMongoRepository.findByType(cellType).size();
				final TFloatList expressionsInCellType = expressionsByCellType.get(cellType);
				final int num_cells_gene_type = expressionsInCellType.size();
				for (final float expressionValue : expressionsInCellType.toArray()) {
					fw.write(gene + "\t" + cellType + "\t" + expressionValue + "\n");
				}
				// N = population size
				// K = successes in the population
				// n = sample size
				// k = successes in the sample
				//
				// K = number of single cells that have GRIA
				// n = number of single cells of type X
				// k = number of single cells that have GRIA and that are of type X

				final int N = num_cells_total;

				final int K = num_cells_gene_total;
				final int n = num_cells_type_total;
				final int k = num_cells_gene_type;
				final HypergeometricDistribution hg = new HypergeometricDistribution(N, K, n);
				double p = hg.upperCumulativeProbability(k);
				if (p < 0.0) {
					p = 0.0;
				}
				fw2.write(gene + "\t" + cellType + "\t" + p + "\t" + N + "\t" + K + "\t" + n + "\t" + k + "\n");

			}
			fw.close();
			fw2.close();
		} catch (final IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void cellTypesFisherTest() {

		try {
			final FileWriter fw = new FileWriter(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_15-41-04_simpleScore_Original\\fisher_test_for_cell_types.txt");
			final Set<String> genes = Files.readAllLines(new File(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_15-41-04_simpleScore_Original\\total_genes.txt")
							.toPath())
					.stream().collect(Collectors.toSet());
			final List<String> significantCellTypes = Files.readAllLines(new File(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_15-41-04_simpleScore_Original\\significant_cell_types.txt")
							.toPath());
			final int num_cells_total = (int) singleCellMongoRepository.count();
			final TObjectIntMap<String> cellsPerGene = new TObjectIntHashMap<String>();
			for (final String significantCellType : significantCellTypes) {
				double chiSquared = 0.0;
				final long num_cells_type_total = singleCellMongoRepository.countByType(significantCellType);

				final List<String> genesByCellType = mongoBaseService.getGenesByCellType(significantCellType, "HCL");
				final int numGenesForType = genes.size();
				if (numGenesForType <= 0) {
					continue;
				}
				int num_significants = 0;
				for (final String gene : genes) {

					final long num_cells_gene_type = expressionMongoRepository.countByGeneAndCellTypeAndProjectTag(gene,
							significantCellType, "HCL");

					int num_cells_gene_total = -1;
					if (cellsPerGene.containsKey(gene)) {
						num_cells_gene_total = cellsPerGene.get(gene);
					} else {
						num_cells_gene_total = (int) expressionMongoRepository.countByGene(gene);
						cellsPerGene.put(gene, num_cells_gene_total);
					}

					// K = successes in the population
					// n = sample size
					// k = successes in the sample
					//
					// K = number of single cells that have GRIA
					// n = number of single cells of type X
					// k = number of single cells that have GRIA and that are of type X

					final int N = num_cells_total;

					final int K = num_cells_gene_total;
					final int n = (int) num_cells_type_total;
					final int k = (int) num_cells_gene_type;
					final HypergeometricDistribution hg = new HypergeometricDistribution(N, K, n);
					double p = hg.upperCumulativeProbability(k);
					if (p < 0.0) {
						p = 0.0;
					}
					if (p < 0.001) {
						num_significants++;
					}
					fw.write(gene + "\t" + significantCellType + "\t" + num_cells_gene_type + "\t" + p + "\n");
					fw.flush();
					if (p == 0.0) {
						p = 1.0 / Double.MAX_VALUE;
						// ignore it for now
						continue;
					}
					chiSquared += Math.log(p);
				}
				chiSquared = chiSquared * -2;

				final double degreesOfFreedom = 2 * numGenesForType;
				final ChiSquaredDistribution chiSquaredDistribution = new ChiSquaredDistribution(degreesOfFreedom);
//				double combinedPValue = chiSquaredDistribution.density(chiSquared);
				final double combinedPValue = 1 - chiSquaredDistribution.cumulativeProbability(chiSquared);
//				combinedPValue = chiSquaredDistribution.inverseCumulativeProbability(chiSquared);
				fw.write(significantCellType + "\t" + combinedPValue + "\t" + num_significants + "\n");
				System.out.println(significantCellType + "\t" + combinedPValue);
				fw.flush();
			}
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void cellTypesFisherTestWithDatabaseUtil() {

		try {
			final FileWriter fw = new FileWriter(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_15-41-04_simpleScore_Original\\fisher_test_for_cell_types.txt");
			final Set<String> genes = Files.readAllLines(new File(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_15-41-04_simpleScore_Original\\total_genes.txt")
							.toPath())
					.stream().collect(Collectors.toSet());
			final List<String> significantCellTypes = Files.readAllLines(new File(
					"C:\\Users\\salvador\\eclipse-workspace\\pctsea-parent\\pctseaweb\\shinyR\\data\\2021-04-21_15-41-04_simpleScore_Original\\significant_cell_types.txt")
							.toPath());
			final int num_cells_total = (int) singleCellMongoRepository.count();
			final TObjectIntMap<String> cellsPerGene = new TObjectIntHashMap<String>();
			for (final String significantCellType : significantCellTypes) {
				double chiSquared = 0.0;
				final long num_cells_type_total = singleCellMongoRepository.countByType(significantCellType);

				final int numGenesForType = genes.size();
				if (numGenesForType <= 0) {
					continue;
				}
				int num_significants = 0;
				for (final String gene : genes) {

					final long num_cells_gene_type = CellTypeAndGenesDBUtil
							.getInstance(cellTypeGeneRepo, expressionMongoRepository)
							.countCellsByGeneAndCellType("HCL", significantCellType, gene);

					int num_cells_gene_total = -1;
					if (cellsPerGene.containsKey(gene)) {
						num_cells_gene_total = cellsPerGene.get(gene);
					} else {
						num_cells_gene_total = (int) expressionMongoRepository.countByGene(gene);
						cellsPerGene.put(gene, num_cells_gene_total);
					}

					// K = successes in the population
					// n = sample size
					// k = successes in the sample
					//
					// K = number of single cells that have GRIA
					// n = number of single cells of type X
					// k = number of single cells that have GRIA and that are of type X

					final int N = num_cells_total;

					final int K = num_cells_gene_total;
					final int n = (int) num_cells_type_total;
					final int k = (int) num_cells_gene_type;
					final HypergeometricDistribution hg = new HypergeometricDistribution(N, K, n);
					double p = hg.upperCumulativeProbability(k);
					if (p < 0.0) {
						p = 0.0;
					}
					if (p < 0.001) {
						num_significants++;
					}
					fw.write(gene + "\t" + significantCellType + "\t" + num_cells_gene_type + "\t" + p + "\n");
					fw.flush();
					if (p == 0.0) {
						p = 1.0 / Double.MAX_VALUE;
						// ignore it for now
						continue;
					}
					chiSquared += Math.log(p);
				}
				chiSquared = chiSquared * -2;

				final double degreesOfFreedom = 2 * numGenesForType;
				final ChiSquaredDistribution chiSquaredDistribution = new ChiSquaredDistribution(degreesOfFreedom);
//				double combinedPValue = chiSquaredDistribution.density(chiSquared);
				final double combinedPValue = 1 - chiSquaredDistribution.cumulativeProbability(chiSquared);
//				combinedPValue = chiSquaredDistribution.inverseCumulativeProbability(chiSquared);
				fw.write(significantCellType + "\t" + combinedPValue + "\t" + num_significants + "\n");
				System.out.println(significantCellType + "\t" + combinedPValue);
				fw.flush();
			}
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
			fail();
		}
	}
}
