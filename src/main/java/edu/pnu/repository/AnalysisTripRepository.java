package edu.pnu.repository;

import edu.pnu.domain.AnalysisTrip;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisTripRepository extends JpaRepository<AnalysisTrip, Long> {
    Stream<AnalysisTrip> streamByEpc_Csv_FileId(Long fileId);

    long countByEpc_Csv_FileId(Long fileId);
}
