package edu.scripps.yates.pctsea.db.datasets.singlecellshuman;

import java.util.Set;

import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.CloseableIterator;

import edu.scripps.yates.pctsea.db.Expression;
import edu.scripps.yates.pctsea.db.ExpressionMongoRepository;
import edu.scripps.yates.pctsea.db.MongoBaseService;
import edu.scripps.yates.utilities.progresscounter.ProgressCounter;
import edu.scripps.yates.utilities.progresscounter.ProgressPrintingType;
import gnu.trove.set.hash.THashSet;

public class GeneToUpperCase {

	private final MongoBaseService mbs;
	private final ExpressionMongoRepository emr;

	public GeneToUpperCase(MongoBaseService mbs, ExpressionMongoRepository emr) {
		this.mbs = mbs;
		this.emr = emr;
	}

	public void run() {
		final MongoTemplate mongoTemplate = mbs.getMongoTemplate();

		int totalModified = 0;
		int totalChecked = 0;
		final int bulksize = 10;
		int updates = 0;
		final CloseableIterator<Expression> expressionIterator = mongoTemplate.stream(new Query(), Expression.class);

//		final Stream<Expression> expressionStream = org.springframework.data.util.StreamUtils
//				.createStreamFromIterator(expressionIterator);
		final Long total = 308072529l;
		final ProgressCounter counter = new ProgressCounter(total, ProgressPrintingType.PERCENTAGE_STEPS, 1, true);
		BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, Expression.class);
		final Set<String> totalgenes = new THashSet<String>();
		final Set<String> genes = new THashSet<String>();
		Expression expression = null;
		while ((expression = expressionIterator.next()) != null) {

			counter.increment();
			final String printIfNecessary = counter.printIfNecessary();
			if (!"".equals(printIfNecessary)) {
				System.out.println(printIfNecessary);
				System.out.println("modified " + totalModified + " in total");
			}

			totalChecked++;
			if (!expression.getGene().toUpperCase().equals(expression.getGene())
					&& !totalgenes.contains(expression.getGene()) && !genes.contains(expression.getGene())) {
				genes.add(expression.getGene());

				final Query query = new Query().addCriteria(new Criteria("gene").is(expression.getGene()));
				final Update update = new Update().set("gene", expression.getGene().toUpperCase());
				bulkOps = bulkOps.updateMulti(query, update);

				updates++;
			}
			if (updates == bulksize) {
				System.out.println("Submitting updates for changing " + genes.size() + " genes...");
				totalgenes.addAll(genes);
				genes.clear();
				final int modifiedCount = bulkOps.execute().getModifiedCount();
				totalModified += modifiedCount;
				System.out.println(modifiedCount + " modified. " + totalModified + " in total, " + totalgenes.size()
						+ " modified  in total");
				bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, Expression.class);
				updates = 0;
			}
		}
		if (updates > 0) {
			System.out.println("Submitting lattest updates for changing " + genes.size() + " genes...");
			totalgenes.addAll(genes);
			genes.clear();
			final int modifiedCount = bulkOps.execute().getModifiedCount();
			totalModified += modifiedCount;
			System.out.println(modifiedCount + " modified. " + totalModified + " in total");
		}
		System.out.println(totalChecked + " checked.");

	}
}
