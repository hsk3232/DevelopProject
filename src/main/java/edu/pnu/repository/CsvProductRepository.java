package edu.pnu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.CsvProduct;

public interface CsvProductRepository extends JpaRepository<CsvProduct, Long> {
	// CSV 파싱 시 중복된 상품 정보를 찾고, Epc에 연결할 productId를 조회하기 위한 쿼리
    @Query("""
    		SELECT cp 
    		FROM CsvProduct cp 
    		WHERE cp.csv.fileId = :fileId
    		""")
    List<CsvProduct> findAllByFileId(@Param("fileId") Long fileId);

}
