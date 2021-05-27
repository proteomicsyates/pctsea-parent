package edu.scripps.yates.pctsea.db;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CellTypeAndGeneMongoRepository extends MongoRepository<CellTypeAndGene, String> {

	public CellTypeAndGene findByDatasetTagAndCellTypeAndGene(String datasetTag, String cellType, String gene);
}
