package edu.pnu.repository;

import edu.pnu.domain.Epc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpcRepository extends JpaRepository<Epc, Long> {
    
	// CsvSaveService.postCsvAndTriggerAsyncProcessing()
	// CSV 파싱 시 중복 체크를 위해 파일 내 모든 epcCode를 조회
    @Query("""
    		SELECT e.epcCode 
    		FROM Epc e 
    		WHERE e.csvFile.fileId = :fileId
    		""")
    Set<String> findAllEpcCodesByFileId(@Param("fileId") Long fileId);
    
    
    // CsvSaveService.postCsvAndTriggerAsyncProcessing()
    //	Map 반환을 위한 쿼리
    @Query("""
    		SELECT e.epcCode, e 
    		FROM Epc e 
    		WHERE e.csvFile.fileId = :fileId
    		""")
    List<Object[]> findAllByFileIdForMap(@Param("fileId") Long fileId);

    
    // CsvSaveService.postCsvAndTriggerAsyncProcessing()
    // Default 메서드를 사용하여 Map 변환 로직 캡슐화
    default Map<String, Epc> findAllByFileIdAsMap(Long fileId) {
        return findAllByFileIdForMap(fileId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // Key: epcCode
                        row -> (Epc) row[1]
                ));
    }

}
