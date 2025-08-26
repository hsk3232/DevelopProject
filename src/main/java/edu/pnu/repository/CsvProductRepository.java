package edu.pnu.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    
    // CsvSaveService.postCsvAndTriggerAsyncProcessing()
    // Key-Value 형태의 Map을 반환하기 위해 List<Object[]>를 먼저 조회
    @Query("""
    		SELECT p.epcCompany, p.epcProduct, p 
    		FROM CsvProduct p 
    		WHERE p.csv.fileId = :fileId
    		""")
    List<Object[]> findAllByFileIdForMap(@Param("fileId") Long fileId);

    
    // Default 메서드를 사용하여 서비스 레이어의 변환 로직을 캡슐화
    default Map<String, CsvProduct> findAllByFileIdAsMap(Long fileId) {
        return findAllByFileIdForMap(fileId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0] + "|" + (String) row[1], // Key: company|product
                        row -> (CsvProduct) row[2]
                ));
    }

}
