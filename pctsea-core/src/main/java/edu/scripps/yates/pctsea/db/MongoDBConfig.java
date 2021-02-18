package edu.scripps.yates.pctsea.db;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.mongodb.client.MongoClient;

import edu.scripps.yates.pctsea.utils.CellTypeBranchConverter;

@Configuration
public class MongoDBConfig {
	@Autowired
	private MongoProperties mongoProperties;

	@Autowired
	private MongoClient mongoClient;

	@Bean
	public MongoTemplate mongoTemplate() {

		final MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, mongoProperties.getDatabase());
		final MappingMongoConverter mongoMapping = (MappingMongoConverter) mongoTemplate.getConverter();
		mongoMapping.setCustomConversions(customConversions()); // tell mongodb to use the custom converters
		mongoMapping.afterPropertiesSet();
		return mongoTemplate;

	}

	public MongoCustomConversions customConversions() {
		return new MongoCustomConversions(Collections.singletonList(new CellTypeBranchConverter()));
	}
}
