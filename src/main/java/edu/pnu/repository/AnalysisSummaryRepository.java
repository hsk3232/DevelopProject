package edu.pnu.repository;

import edu.pnu.domain.AnalysisSummary;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisSummaryRepository extends JpaRepository<AnalysisSummary, Long> {
    AnalysisSummary findByCsv_FileId(Long fileId);

}
