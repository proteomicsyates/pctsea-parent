package edu.scripps.yates.pctsea.db;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectMongoRepository extends MongoRepository<Project, String> {

	List<Project> findByName(String name);

	List<Project> findByTag(String tag);
}
