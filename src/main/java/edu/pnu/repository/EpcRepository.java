package edu.pnu.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.Epc;

public interface EpcRepository extends JpaRepository<Epc, Long> {
    // CSV 파싱 시 중복 체크를 위해 파일 내 모든 epcCode를 조회
    @Query("""
    		SELECT e.epcCode 
    		FROM Epc e 
    		WHERE e.csv.fileId = :fileId
    		""")
    Set<String> findAllEpcCodesByFileId(@Param("fileId") Long fileId);
}
