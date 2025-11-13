package edu.pnu.repository;

import edu.pnu.domain.BeAnalysis;
import edu.pnu.dto.DashboardDTO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BeAnalysisRepository extends JpaRepository<BeAnalysis, Long> {

    long countByEventHistory_CsvFile_FileId(long fileId);


    //	[DashboardService].getNodeList
    List<BeAnalysis> findByAnalyzedTrip_RoadIdIn(List<Long> eventIds);

    @Query("""
	            SELECT be.anomalyType
	            FROM BeAnalysis be JOIN be.eventHistory eh
	            WHERE eh.csvFile.fileId = :fileId AND be.anomalyType IS NOT NULL
	            ORDER BY be.anomalyType
	            """)
    List<String> findDistincByFileId(@Param("fileId") Long fileId);

    // [StatisticsFindService].getByProduct
    @Query("""
	            SELECT new edu.pnu.dto.DashboardDTO.ByProductInfo(
	                p.csvProductName,
	                SUM(CASE WHEN be.anomalyType = 'fake' THEN 1L ELSE 0L END),
	                SUM(CASE WHEN be.anomalyType = 'tamper' THEN 1L ELSE 0L END),
	                SUM(CASE WHEN be.anomalyType = 'clone' THEN 1L ELSE 0L END),
	                SUM(CASE WHEN be.anomalyType = 'other' THEN 1L ELSE 0L END),
	                COUNT(be)
	            )
	            FROM BeAnalysis be
	            JOIN be.eventHistory eh
	            JOIN eh.epc e
	            JOIN CsvProduct p
	            WHERE eh.csvFile.fileId = :fileId
	            GROUP BY p.csvProductName
	            ORDER BY p.csvProductName
	            """)
    List<DashboardDTO.ByProductInfo> countByProduct(@Param("fileId") Long fileId);
}
