package edu.pnu.repository;

import edu.pnu.domain.CsvRoute;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisTripRepository extends JpaRepository<CsvRoute, Long> {
    Stream<CsvRoute> streamByEpc_Csv_FileId(Long fileId);

    long countByEpc_Csv_FileId(Long fileId);
}
