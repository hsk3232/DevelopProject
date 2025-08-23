package edu.pnu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.AnalysisSummary;

public interface AnalysisSummaryRepository extends JpaRepository<AnalysisSummary, Long> {

}
