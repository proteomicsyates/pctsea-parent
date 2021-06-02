package edu.scripps.yates.pctsea.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.mongodb.BasicDBObject;

import edu.scripps.yates.pctsea.PCTSEA;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

@Service
public class MongoBaseService {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(MongoBaseService.class);

	@Inject
	MongoTemplate mongoTemplate;
	@Inject
	ExpressionMongoRepository emr;
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

	public List<String> getGenesByCellType(String cellType, Set<String> datasets) {
		final Criteria criteria = Criteria.where("cellType").is(cellType);
		if (datasets != null && !datasets.isEmpty()) {
			criteria.and("projectTag").in(datasets);
		}
		final Query query = new Query();
		query.fields().exclude("cellType").exclude("cellName").exclude("expression");
		query.addCriteria(criteria);

		final List<String> genes = mongoTemplate.findDistinct(query, "gene", Expression.class, String.class);
		return genes;
	}

	public Map<String, List<Expression>> getExpressionByGenesByInCriteria(Set<String> genes, Set<String> datasets) {

		final Criteria criteria = new Criteria("gene").in(genes);
		final Query query = new Query();
		query.fields().exclude("cellType").exclude("cell");
		query.addCriteria(criteria);
		if (datasets != null && !datasets.isEmpty()) {
			criteria.and("projectTag").in(datasets);
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

	public Map<String, List<Expression>> getExpressionByGenesOneByOne(Set<String> genes, Set<String> datasets) {
		final Map<String, List<Expression>> ret = new THashMap<String, List<Expression>>();

		final ProgressCounter counter = new ProgressCounter(genes.size(), ProgressPrintingType.PERCENTAGE_STEPS, 1,
				true);
		counter.setSuffix("retrieving expressions from database");
		for (final String gene : genes) {
			counter.increment();
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				PCTSEA.logStatus(printIfNecessary);
			}
			if (datasets != null && !datasets.isEmpty()) {
				for (final String dataset : datasets) {
					final List<Expression> expressions = emr.findByGeneAndProjectTag(gene, dataset);
					for (final Expression expression : expressions) {
						if (!ret.containsKey(gene)) {
							final ArrayList<Expression> list = new ArrayList<Expression>();
							list.add(expression);
							ret.put(gene, list);
						} else {
							ret.get(gene).add(expression);
						}
					}
				}
			} else {
				PCTSEA.logStatus("Gene: " + gene);
				final List<Expression> expressions = emr.findByGene(gene);
				PCTSEA.logStatus(expressions.size() + " expressions");
				for (final Expression expression : expressions) {
					if (!ret.containsKey(gene)) {
						final ArrayList<Expression> list = new ArrayList<Expression>();
						list.add(expression);
						ret.put(gene, list);
					} else {
						ret.get(gene).add(expression);
					}
				}
			}
		}
		return ret;
	}

	public List<Expression> getExpressionByGene(String gene, Set<String> datasets) {
		final List<Expression> ret = new ArrayList<Expression>();

		if (datasets != null && !datasets.isEmpty()) {
			for (final String dataset : datasets) {
				final List<Expression> expressions = emr.findByGeneAndProjectTag(gene, dataset);
				ret.addAll(expressions);
			}
		} else {
			final List<Expression> expressions = emr.findByGene(gene);
			ret.addAll(expressions);
		}

		return ret;
	}

	public long countExpressionsByGeneAndDatasets(String gene, String dataset) {
		if (dataset != null) {

			final long count = emr.countByGeneAndProjectTag(gene, dataset);
			if (count > 0) {
				return count;
			}

		} else {
			final long count = emr.countByGene(gene);
			return count;
		}

		return 0l;
	}

	public long countExpressionsByGeneAndDatasets(String gene, Set<String> datasets) {
		if (datasets != null && !datasets.isEmpty()) {
			for (final String dataset : datasets) {
				final long count = emr.countByGeneAndProjectTag(gene, dataset);
				if (count > 0) {
					return count;
				}
			}
		} else {
			final long count = emr.countByGene(gene);
			return count;
		}

		return 0l;
	}

	public List<Expression> getExpressionByGenes(Collection<String> genes, Dataset dataset) {
		final List<Expression> ret = new ArrayList<Expression>();

		if (dataset != null) {
			final Query query = Query.query(
					Criteria.where("gene").in(genes).andOperator(Criteria.where("projectTag").is(dataset.getTag())));
			final List<Expression> expressions = mongoTemplate.find(query, Expression.class);
			ret.addAll(expressions);

		} else {
			final Query query = Query.query(Criteria.where("gene").in(genes));
			final List<Expression> expressions = mongoTemplate.find(query, Expression.class);
			ret.addAll(expressions);
		}

		return ret;
	}

	public List<Expression> getExpressionByGene(String gene, Dataset dataset) {
		final List<Expression> ret = new ArrayList<Expression>();

		if (dataset != null) {
			final List<Expression> expressions = emr.findByGeneAndProjectTag(gene, dataset.getTag());
			ret.addAll(expressions);

		} else {
			final List<Expression> expressions = emr.findByGene(gene);
			ret.addAll(expressions);
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
			PCTSEA.logStatus("BulkWriteOperation Exception ::  " + ex, LogLevel.ERROR);
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
			PCTSEA.logStatus("BulkWriteOperation Exception ::  " + ex, LogLevel.ERROR);
			return null;
		}
	}

	public List<String> getGenesByCellType(String cellType, String projectTag) {
		final Set<String> projectTags = new THashSet<String>();
		projectTags.add(projectTag);
		return getGenesByCellType(cellType, projectTags);
	}

	public void getExpressionByGenes(Collection<String> genes, Collection<Dataset> datasets,
			DocumentCallbackHandler documentProcessor) {
		final List<Expression> ret = new ArrayList<Expression>();

		if (datasets != null) {
			final Set<String> datasetTags = datasets.stream().map(dt -> dt.getTag()).collect(Collectors.toSet());
			final Query query = Query
					.query(Criteria.where("gene").in(genes).andOperator(Criteria.where("projectTag").in(datasetTags)));
			mongoTemplate.executeQuery(query, "expression", documentProcessor);

		} else {
			final Query query = Query.query(Criteria.where("gene").in(genes));
			final List<Expression> expressions = mongoTemplate.find(query, Expression.class);
			ret.addAll(expressions);
		}

	}

}
