package edu.pnu.repository;


import edu.pnu.domain.CsvFile;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CsvFileRepository extends JpaRepository<CsvFile, Long> {
    @Query("""
                SELECT c
                FROM CsvFile c
                JOIN FETCH c.member m
                WHERE
                    m.assetLocation.locationId = :locationId
                    AND (:cursor IS NULL OR c.fileId < :cursor)
                    AND (:search IS NULL OR LOWER(c.fileName) LIKE LOWER(CONCAT('%', :search, '%')))
                ORDER BY c.fileId DESC
            """)
    List<CsvFile> findCsvListByCriteria(
            @Param("locationId") Long locationId,
            @Param("cursor") Long cursor,
            @Param("search") String search,
            Pageable pageable);

    // DataShareService에서 가장 최근 파일을 찾기 위한 쿼리
    Optional<CsvFile> findTopByOrderByFileIdDesc();


}
