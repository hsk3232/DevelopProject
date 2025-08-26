package edu.pnu.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.CsvLocation;

public interface CsvLocationRepository extends JpaRepository<CsvLocation, Long> {
	// CSV 파싱 시 중복 체크를 위해 파일 내 모든 locationId를 조회
		
	@Query("""			
			SELECT cl.locationId 
			FROM CsvLocation cl
			""")
    Set<Long> findAllLocationIds();
}
