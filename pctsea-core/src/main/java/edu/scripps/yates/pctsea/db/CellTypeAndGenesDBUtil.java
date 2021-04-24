package edu.scripps.yates.pctsea.db;

/**
 * This class will function as a proxy between the database and the user that
 * ask how many genes are per cell type. Query results will be stored in a new
 * collection in the database so they can be retrieved quicker
 * 
 * @author salvador
 *
 */
public class CellTypeAndGenesDBUtil {
	public static CellTypeAndGenesDBUtil instance;
	private final CellTypeAndGeneMongoRepository cellTypeGeneRepo;
	private final ExpressionMongoRepository expressionMongoRepository;

	private CellTypeAndGenesDBUtil(CellTypeAndGeneMongoRepository cellTypeGeneRepo,
			ExpressionMongoRepository expressionMongoRepository) {
		this.cellTypeGeneRepo = cellTypeGeneRepo;
		this.expressionMongoRepository = expressionMongoRepository;
	}

	public static CellTypeAndGenesDBUtil getInstance(CellTypeAndGeneMongoRepository cellTypeGeneRepo,
			ExpressionMongoRepository expressionMongoRepository) {
		if (instance == null) {
			instance = new CellTypeAndGenesDBUtil(cellTypeGeneRepo, expressionMongoRepository);
		}
		return instance;
	}

	public long countCellsByGeneAndCellType(String datasetTag, String cellType, String gene) {
		final CellTypeAndGene cellTypeAndGene = this.cellTypeGeneRepo.findByDatasetTagAndCellTypeAndGene(datasetTag,
				cellType, gene);
		if (cellTypeAndGene == null) {
			final CellTypeAndGene entity = new CellTypeAndGene(null, datasetTag, gene, cellType, -1);
			this.cellTypeGeneRepo.save(entity);
			return 0;
		} else {
			long count = cellTypeAndGene.getCellCount();
			if (count == -1) {
				return 0;
			} else if (count == 0) {
				count = expressionMongoRepository.countByGeneAndCellTypeAndProjectTag(gene, cellType, datasetTag);
				if (count == 0) {
					count = -1; // to distinguish when it was queried and it is 0 than when it has not been
								// queried
				}
				final CellTypeAndGene entity = new CellTypeAndGene(null, datasetTag, gene, cellType, count);
				this.cellTypeGeneRepo.save(entity);
				System.out.println(entity.getId());
				return count;
			} else {
				return count;
			}
		}
	}
}
