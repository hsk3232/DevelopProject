package edu.pnu.repository;

import edu.pnu.domain.AiAnalysis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    long countByEventHistory_Csv_FileId(long fileId);
}
