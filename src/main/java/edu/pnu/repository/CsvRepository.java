package edu.pnu.repository;


import edu.pnu.domain.Csv;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvRepository extends JpaRepository<Csv, Long> {
    // Manager의 파일 목록 조회를 위한 커서 기반 페이징 쿼리 (검색 포함)
    List<Csv> findByMember_Location_LocationIdAndFileIdLessThanAndFileNameContainingIgnoreCaseOrderByFileIdDesc(
            Long locationId, Long cursor, String fileName, Pageable pageable);

    // Manager의 파일 목록 조회를 위한 커서 기반 페이징 쿼리 (검색 없음)
    List<Csv> findByMember_Location_LocationIdAndFileIdLessThanOrderByFileIdDesc(
            Long locationId, Long cursor, Pageable pageable);

    // 첫 페이지 조회를 위한 쿼리 (검색 포함)
    List<Csv> findByMember_Location_LocationIdAndFileNameContainingIgnoreCaseOrderByFileIdDesc(
            Long locationId, String fileName, Pageable pageable);

    // 첫 페이지 조회를 위한 쿼리 (검색 없음)
    List<Csv> findByMember_Location_LocationIdOrderByFileIdDesc(
            Long locationId, Pageable pageable);

    // DataShareService에서 가장 최근 파일을 찾기 위한 쿼리
    Optional<Csv> findTopByOrderByFileIdDesc();


}
