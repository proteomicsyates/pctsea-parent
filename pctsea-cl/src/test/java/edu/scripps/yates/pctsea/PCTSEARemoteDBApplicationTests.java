package edu.scripps.yates.pctsea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

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
		properties = { "headles=false", //
				// to force to conect to the remote DB with tunnel
				"spring.config.location=classpath:/application-remoteTunnel.properties" })
public class PCTSEARemoteDBApplicationTests {
	@Autowired
	SingleCellMongoRepository scRepo;

	@Test
	public void Test() throws IOException {
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

	}

}
