package edu.scripps.yates.pctsea.db;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExpressionMongoRepository extends MongoRepository<Expression, String> {
//	public List<Expression> findByGene(String gene);

	// in this way, we ensure that the index using collation that ignore case is
	// used
//	@Query(collation = "{locale:'en',strength:2}")
	public List<Expression> findByGene(String gene);

	public Expression findFirstByGene(String gene);

	public List<Expression> findByGeneLikeAndProjectTagLike(String gene, String projectTag);

	public List<Expression> findByGeneAndProjectTag(String gene, String projectTag);

//	@Query("{'cellName': ?0}")
	public List<Expression> findByCellName(String cellName);

//	@Query("{'cellType': ?0}")
	public List<Expression> findByCellType(String cellType);

	public long countByGeneAndProjectTag(String gene, String projectTag);

	public long countByGene(String gene);

	public long countByGeneAndCellTypeAndProjectTag(String gene, String cellType, String projectTag);

	public long countByCellTypeAndProjectTag(String cellType, String projectTag);

}
