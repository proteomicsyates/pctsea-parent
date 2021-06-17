package edu.scripps.yates.pctsea.db;

import java.util.Locale;

import javax.inject.Inject;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.stereotype.Component;

@Component
public class MongoDBCreator {
	private final static Logger log = LoggerFactory.getLogger(MongoDBCreator.class);
	@Inject
	MongoTemplate template;

	@EventListener(ContextRefreshedEvent.class)
	public void initIndicesAfterStartup() {
		log.info("Initializing MongoDB structure:");
		if (!template.collectionExists(Expression.class)) {
			log.info("Creating document collection for " + Expression.class);
			template.createCollection(Expression.class);
		} else {
			log.info("Document collection for " + Expression.class + " already exists");
		}
		if (!template.collectionExists(PctseaRunLog.class)) {
			log.info("Creating document collection for " + PctseaRunLog.class);
			template.createCollection(PctseaRunLog.class);
			log.info("Document collection for " + PctseaRunLog.class + " already exists");
		}
		if (!template.collectionExists(CellTypeAndGene.class)) {
			log.info("Creating document collection for " + CellTypeAndGene.class);
			template.createCollection(CellTypeAndGene.class);
			log.info("Document collection for " + CellTypeAndGene.class + " already exists");
		}
//		ensureExpressionGeneIndex();
//		ensureExpressionProjectIndex();
		ensureExpressionGeneAndProjectTagCompoundIndex();
		ensureCellTypeAndGeneAndProjectTagCompoundIndex();
		ensureExpressionCellTypeIndex();
		ensureExpressionCellNameIndex();
		if (!template.collectionExists(SingleCell.class)) {
			log.info("Creating document collection for " + SingleCell.class);
			template.createCollection(SingleCell.class);
			log.info("Document collection for " + SingleCell.class + " already exists");

		}
		ensureSingleCellTypeIndex();
		ensureSingleCellBiomaterialIndex();
		ensureSingleCellNameIndex();
		ensureSingleCellDatasetTagIndex();
		if (!template.collectionExists(Dataset.class)) {
			log.info("Creating document collection for " + Dataset.class);
			template.createCollection(Dataset.class);
			log.info("Document collection for " + Dataset.class + " already exists");

		}
		ensureProjectNameIndex();
		ensureProjectTagIndex();

		log.info("Database looks ready!");
	}

	private void ensureExpressionGeneAndProjectTagCompoundIndex() {
		logIndexCreation("name and projectTag", Expression.class);
		final IndexDefinition index = new CompoundIndexDefinition(
				new Document().append("gene", 1).append("projectTag", 1))
						.named("expression_gene_projectTag_compound_index");
		template.indexOps(Expression.class).ensureIndex(index);

	}

	private void ensureCellTypeAndGeneAndProjectTagCompoundIndex() {
		logIndexCreation("cellType gene-cellType-datasetTag", CellTypeAndGene.class);
		final IndexDefinition index = new CompoundIndexDefinition(
				new Document().append("gene", 1).append("datasetTag", 1).append("cellType", 1))
						.named("cellTypeAndGene_gene_cellType_datasetTag_compound_index");
		template.indexOps(CellTypeAndGene.class).ensureIndex(index);

	}

	private void ensureProjectNameIndex() {
		logIndexCreation("name", Dataset.class);
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("name", Direction.ASC).collation(collation).unique()
				.named("dataset_name_unique_index");
		template.indexOps(Dataset.class).ensureIndex(index);
	}

	private void logIndexCreation(String field, Class documentClass, boolean withCollation) {
		if (withCollation) {
			log.info("Making sure index on '" + field + "' field of collection " + documentClass.getName()
					+ " with collation is present.");
		} else {
			log.info("Making sure index on '" + field + "' field of collection " + documentClass.getName()
					+ " is present.");
		}
	}

	private void logIndexCreation(String field, Class documentClass) {
		logIndexCreation(field, documentClass, false);
	}

	private void ensureProjectTagIndex() {
		logIndexCreation("tag", Dataset.class);
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("tag", Direction.ASC).collation(collation).unique()
				.named("dataset_tag_unique_collation_index");
		template.indexOps(Dataset.class).ensureIndex(index);
	}

	private void ensureSingleCellNameIndex() {
		logIndexCreation("name", SingleCell.class);
		final Index index = new Index("name", Direction.ASC).unique().named("singleCell_name_unique_index");
		template.indexOps(SingleCell.class).ensureIndex(index);

	}

	private void ensureSingleCellDatasetTagIndex() {
		logIndexCreation("datasetTag", SingleCell.class);
		final Index index = new Index("datasetTag", Direction.ASC).named("singleCell_datasetTag_index");
		template.indexOps(SingleCell.class).ensureIndex(index);

	}

	private void ensureSingleCellTypeIndex() {
		logIndexCreation("type", SingleCell.class, true);
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("type", Direction.ASC).collation(collation)
				.named("singleCell_type_collation_index");
		template.indexOps(SingleCell.class).ensureIndex(index);
		// with not collation
		logIndexCreation("type", SingleCell.class, false);
		final Index indexWithoutCollation = new Index().on("type", Direction.ASC).named("singleCell_type_index");
		template.indexOps(SingleCell.class).ensureIndex(indexWithoutCollation);
	}

	private void ensureSingleCellBiomaterialIndex() {
		logIndexCreation("biomaterial", SingleCell.class, true);
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("biomaterial", Direction.ASC).collation(collation)
				.named("singleCell_biomaterial_collation_index");
		template.indexOps(SingleCell.class).ensureIndex(index);
	}

	private void ensureExpressionCellNameIndex() {
		logIndexCreation("cellName", Expression.class);
		final Index index = new Index().on("cellName", Direction.ASC).named("expression_cellName_index");
		template.indexOps(Expression.class).ensureIndex(index);

	}

	private void ensureExpressionCellTypeIndex() {
		logIndexCreation("cellType", Expression.class, true);
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("cellType", Direction.ASC).collation(collation)
				.named("expression_cellType_collation_index");
		template.indexOps(Expression.class).ensureIndex(index);
	}

	private void ensureExpressionGeneIndex() {
		logIndexCreation("gene", Expression.class, true);
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("gene", Direction.ASC).collation(collation)
				.named("expression_gene_collation_index");
		template.indexOps(Expression.class).ensureIndex(index);

		logIndexCreation("gene", Expression.class, false);
		final Index index2 = new Index().on("gene", Direction.ASC).named("expression_gene_index");
		template.indexOps(Expression.class).ensureIndex(index2);
	}

	private void ensureExpressionProjectIndex() {
		logIndexCreation("projectTag", Expression.class, true);
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index indexWithCollation = new Index().on("projectTag", Direction.ASC).collation(collation)
				.named("expression_projectTag_collation_index");
		template.indexOps(Expression.class).ensureIndex(indexWithCollation);
// without collation, simple
		logIndexCreation("projectTag", Expression.class, false);
		final Index indexWithouCollation = new Index().on("projectTag", Direction.ASC)
				.named("expression_projectTag_index");
		final String indexWithoutCollationName = indexWithCollation.getIndexOptions().getString("name");
		template.indexOps(Expression.class).ensureIndex(indexWithouCollation);
	}
}
