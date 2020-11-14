package edu.scripps.yates.pctsea.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.mongodb.BasicDBObject;

import gnu.trove.map.hash.THashMap;

@Service
public class MongoBaseService {

	@Inject
	MongoTemplate mongoTemplate;

//	public MongoBaseService(@Autowired MongoTemplate mongoTemplate) {
//		this.mongoTemplate = mongoTemplate;
//	}

	public List<Expression> getExpressionByGeneIgnoringCase(String gene) {
		final Collation collation = Collation.of("en").caseLevel(false).strength(2);
		final Query query = new BasicQuery("{ gene : '" + gene + "'}").collation(collation);
		final List<Expression> find = mongoTemplate.find(query, Expression.class);
		return find;
	}

	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}

	public Map<String, List<Expression>> getExpressionByGenes(Set<String> genes, Set<String> datasets) {

		final Criteria criteria = new Criteria("gene").in(genes);
		final Query query = new Query();
		query.fields().exclude("cellType").exclude("cell");
		query.addCriteria(criteria);
		if (datasets != null && !datasets.isEmpty()) {
			criteria.and("datasetTag").in(datasets);
		}
		final List<Expression> expressions = mongoTemplate.find(query, Expression.class);
		final Map<String, List<Expression>> ret = new THashMap<String, List<Expression>>();
		for (final Expression expression : expressions) {
			final String gene = expression.getGene().toUpperCase();
			if (!ret.containsKey(gene)) {
				final ArrayList<Expression> list = new ArrayList<Expression>();
				list.add(expression);
				ret.put(gene, list);
			} else {
				ret.get(gene).add(expression);
			}
		}
		return ret;
	}

	public List<Expression> saveExpressions(List<Expression> entities) {
		Assert.notNull(entities, "Entity must not be null!");

		final BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, entities.get(0).getClass());

		/**
		 * Creating MongoConverter object (We need converter to convert Entity Pojo to
		 * BasicDbObject)
		 */
		final MongoConverter converter = mongoTemplate.getConverter();

		BasicDBObject dbObject;

		/**
		 * Handling bulk write exceptions
		 */
		try {

			for (final Expression entity : entities) {
				/**
				 * Get class name for sequence class
				 */
//				final String className = entity.getClass().getSimpleName();
				if (entity.getId() == null) {
					/**
					 * Set Id if entity don't have Id
					 */
					entity.setId(new ObjectId() + "");
					dbObject = new BasicDBObject();

					/*
					 * Converting entity object to BasicDBObject
					 */
					converter.write(entity, dbObject);

					/*
					 * Adding entity (BasicDBObject)
					 */
					bulkOps.insert(dbObject);
				} else {
					throw new IllegalArgumentException("this entity has an id already!");
				}
			}

			/**
			 * Executing the Operation
			 */
			bulkOps.execute();
			return entities;

		} catch (final Exception ex) {
			System.out.println("BulkWriteOperation Exception ::  " + ex);
			return null;
		}
	}

	public List<SingleCell> saveSingleCells(List<SingleCell> entities) {
		Assert.notNull(entities, "Entity must not be null!");

		final BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, entities.get(0).getClass());

		/**
		 * Creating MongoConverter object (We need converter to convert Entity Pojo to
		 * BasicDbObject)
		 */
		final MongoConverter converter = mongoTemplate.getConverter();

		BasicDBObject dbObject;

		/**
		 * Handling bulk write exceptions
		 */
		try {

			for (final SingleCell entity : entities) {
				/**
				 * Get class name for sequence class
				 */
//				final String className = entity.getClass().getSimpleName();
				if (entity.getId() == null) {
					/**
					 * Set Id if entity don't have Id
					 */
					entity.setId(new ObjectId() + "");
					dbObject = new BasicDBObject();

					/*
					 * Converting entity object to BasicDBObject
					 */
					converter.write(entity, dbObject);

					/*
					 * Adding entity (BasicDBObject)
					 */
					bulkOps.insert(dbObject);
				} else {
					throw new IllegalArgumentException("this entity has an id already!");
				}
			}

			/**
			 * Executing the Operation
			 */
			bulkOps.execute();
			return entities;

		} catch (final Exception ex) {
			System.out.println("BulkWriteOperation Exception ::  " + ex);
			return null;
		}
	}
}
