package edu.scripps.yates.pctsea.db;

import java.util.Locale;

import javax.inject.Inject;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.stereotype.Component;

@Component
public class MongoDBCreator {

	@Inject
	MongoTemplate template;

	@EventListener(ContextRefreshedEvent.class)
	public void initIndicesAfterStartup() {

		if (!template.collectionExists(Expression.class)) {
			template.createCollection(Expression.class);
		}
		ensureExpressionGeneIndex();
		ensureExpressionProjectIndex();
		ensureExpressionCellTypeIndex();
		ensureExpressionCellNameIndex();
		if (!template.collectionExists(SingleCell.class)) {
			template.createCollection(SingleCell.class);
		}
		ensureSingleCellTypeIndex();
		ensureSingleCellBiomaterialIndex();
		ensureSingleCellNameIndex();
		if (!template.collectionExists(Dataset.class)) {
			template.createCollection(Dataset.class);
		}
		ensureProjectNameIndex();
		ensureProjectTagIndex();
	}

	private void ensureProjectNameIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("name", Direction.ASC).collation(collation).unique();
		template.indexOps(Dataset.class).ensureIndex(index);
	}

	private void ensureProjectTagIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("tag", Direction.ASC).collation(collation).unique();
		template.indexOps(Dataset.class).ensureIndex(index);
	}

	private void ensureExpressionProjectIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("projectTag", Direction.ASC).collation(collation);
		template.indexOps(Expression.class).ensureIndex(index);
	}

	private void ensureExpressionGeneAndProjectIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("projectTag", Direction.ASC).collation(collation);
		template.indexOps(Expression.class).ensureIndex(index);
	}

	private void ensureSingleCellNameIndex() {
		final Index index = new Index("name", Direction.ASC).unique();
		template.indexOps(SingleCell.class).ensureIndex(index);

	}

	private void ensureSingleCellTypeIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("type", Direction.ASC).collation(collation);
		template.indexOps(SingleCell.class).ensureIndex(index);
	}

	private void ensureSingleCellBiomaterialIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("biomaterial", Direction.ASC).collation(collation);
		template.indexOps(SingleCell.class).ensureIndex(index);
	}

	private void ensureExpressionCellNameIndex() {
		final Index index = new Index().on("cellName", Direction.ASC);
		template.indexOps(Expression.class).ensureIndex(index);

	}

	private void ensureExpressionCellTypeIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("cellType", Direction.ASC).collation(collation);
		template.indexOps(Expression.class).ensureIndex(index);
	}

	private void ensureExpressionGeneIndex() {
		final Collation collation = Collation.of(Locale.ENGLISH).caseLevel(false).strength(2);
		final Index index = new Index().on("gene", Direction.ASC).collation(collation)
				.named("my_gene_index_with_collation");
		template.indexOps(Expression.class).ensureIndex(index);
		final Index index2 = new Index().on("gene", Direction.ASC).named("my_gene_index_simple");
		template.indexOps(Expression.class).ensureIndex(index2);
	}
}
