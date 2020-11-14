package edu.scripps.yates.pctsea.db;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SingleCellMongoRepository extends MongoRepository<SingleCell, String> {

	List<SingleCell> findByName(String name);

	List<SingleCell> findByType(String type);

	List<SingleCell> findByDatasetTag(String datasetTag);
}
