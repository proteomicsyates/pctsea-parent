package edu.scripps.yates.pctsea;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.SingleCell;
import edu.scripps.yates.pctsea.db.SingleCellMongoRepository;

/**
 * 
 * 
 * @author salvador
 *
 */
@RunWith(SpringRunner.class)
@AutoConfigureDataMongo
@SpringBootTest(//
		// using args="test" makes that PCTSEADbApplication don't start as usually (see
		// in its code)
		args = "test", //
		// we don't want a web environment to test
		webEnvironment = WebEnvironment.NONE, //
		properties = { "headles=false" })
public class PCTSEALocalDBApplicationTests {
	@Autowired
	SingleCellMongoRepository scRepo;

	@Autowired
	ExpressionMongoRepository expressionsRepo;

	@Test
	public void testExpressions() {
		try {
			System.out.println("Starting test...");
			final String gene = "A1BG-AS1";
			final String gene2 = "A1bg-AS1";
			final String projectTag = "HCL";
			final List<Expression> expressions = expressionsRepo.findByGene(gene);
			Assert.assertTrue(!expressions.isEmpty());
			System.out.println(expressions.size() + " expressions of gene " + gene);
			final List<Expression> expressions3 = expressionsRepo.findByGene(gene2);
			Assert.assertTrue(expressions3.size() == expressions.size());
			System.out.println(expressions3.size() + " expressions of gene " + gene2);

			final List<Expression> expressions2 = expressionsRepo.findByGeneAndProjectTag(gene, projectTag);
			Assert.assertTrue(!expressions2.isEmpty());
			System.out.println(expressions2.size() + " expressions of gene " + gene + " in project " + projectTag);
		} catch (final Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testSingleCells() {
		try {
			System.out.println("Daniel test");
			final long count = scRepo.count();
			System.out.println(count + " single cells");

			final List<String> brainCellTypes = Files.readAllLines(
					new File("C:\\Users\\salvador\\Desktop\\daniel\\single_cells\\brain_cell_types.txt").toPath());
			System.out.println(brainCellTypes.size() + " cell types");
			final SingleCell sc1 = scRepo.findByName("AdultColon_1.TAGAGAAATAAAAGTTTA").get(0);
			System.out.println(sc1.getId() + "\t" + sc1.getName() + "\t" + sc1.getType());
			final SingleCell sc2 = scRepo.findByName("AdultColon_1.TGATCACCTAGACGCACC").get(0);
			System.out.println(sc2.getId() + "\t" + sc2.getName() + "\t" + sc2.getType());
			final SingleCell sc3 = scRepo.findByName("FetalIntestine_5.GATCTTGAATTAATTTGC").get(0);
			System.out.println(sc3.getId() + "\t" + sc3.getName() + "\t" + sc3.getType());
		} catch (final Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
