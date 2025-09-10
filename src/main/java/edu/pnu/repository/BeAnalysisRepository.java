package edu.pnu.repository;

import edu.pnu.domain.BeAnalysis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeAnalysisRepository extends JpaRepository<BeAnalysis, Long> {

    long countByEventHistory_Csv_FileId(long fileId);
}
