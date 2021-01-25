package edu.scripps.yates.pctsea;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.pctsea.db.DatasetMongoRepository;
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
		args = { "HCL",
				"C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\data_from_article\\dge_rmbatch_data",
				"C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\data_from_article\\annotation_rmbatch_data_revised417",
				"10000" }, //
		// we don't want a web environment to test
		webEnvironment = WebEnvironment.NONE, //
		properties = { "headles=false" //
		// to force to conect to the remote DB with tunnel
//				,"spring.config.location=classpath:/application-remoteTunnel.properties"//
				, "spring.jpa.hibernate.ddl-auto=create" })
public class HumanCellDatasetCreationTest {

	@Autowired
	DatasetMongoRepository projectMongoRepo;
	@Autowired
	SingleCellMongoRepository singleCellMongoRepository;
	@Autowired
	MongoBaseService mongoBaseService;
	int batchSize = 5000;

	@Test
	public void DatasetCreation() {

	}

}
