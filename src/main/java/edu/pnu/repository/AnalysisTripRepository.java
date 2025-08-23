package edu.pnu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.AnalysisTrip;

public interface AnalysisTripRepository extends JpaRepository<AnalysisTrip, Long> {

}
