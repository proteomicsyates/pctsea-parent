package edu.scripps.yates.pctsea.db;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PctseaRunLogRepository extends MongoRepository<PctseaRunLog, String> {

}
