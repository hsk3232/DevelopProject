package edu.pnu.repository;

import edu.pnu.domain.AiAnalysis;
import edu.pnu.domain.EventHistory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    long countByEventHistory_CsvFile_FileId(long fileId);

    Optional<AiAnalysis> findByEventHistory(EventHistory eventHistory);

    List<AiAnalysis> findByEventHistory_EventIdIn(List<Long> eventIds);







    @Query("""
			    SELECT ad
			    FROM AiAnalysis ad
			    JOIN FETCH ad.eventHistory eh
			    JOIN FETCH eh.epc e
			    WHERE eh.csvFile.fileId = :fileId
			    ORDER BY e.epcCode ASC, eh.eventTime ASC
			    """)
    List<AiAnalysis> findDistinctAnomalyTypesByFileId(@Param("fileId") Long fileId);





}
