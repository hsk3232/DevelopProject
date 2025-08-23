package edu.pnu.repository;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.EventHistory;

public interface EventHistoryRepository extends JpaRepository<EventHistory, Long> {
	// Trip 분석 시 대용량 데이터를 메모리 효율적으로 처리하기 위한 Stream 쿼리
    @Query("""
    		SELECT eh 
    		FROM EventHistory eh 
    		WHERE eh.csv.fileId = :fileId 
    		ORDER BY eh.epc.epcId, eh.eventTime
    		
    		""")
    Stream<EventHistory> streamByFileIdOrderByEpcIdAndEventTime(@Param("fileId") Long fileId);
    
    // AI/BE 분석을 위해 파일 내 이상 탐지 대상 이벤트를 조회하는 쿼리
    @Query("""
    		SELECT eh 
    		FROM EventHistory eh 
    		JOIN FETCH eh.epc e 
    		JOIN FETCH eh.csvProduct p 
    		JOIN FETCH eh.csvLocation l 
    		WHERE eh.csv.fileId = :fileId
    		""")
    List<EventHistory> findAllWithDetailsByFileId(@Param("fileId") Long fileId);
}
