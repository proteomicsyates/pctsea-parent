package edu.scripps.yates.pctsea.db;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DatasetMongoRepository extends MongoRepository<Dataset, String> {

	public List<Dataset> findByName(String name);

//
	List<Dataset> findByTag(String tag);
}
