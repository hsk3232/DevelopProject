package edu.pnu.repository;

import edu.pnu.domain.CsvRoute;

import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvRouteRepository extends JpaRepository<CsvRoute, Long> {
    Stream<CsvRoute> streamByEpc_CsvFile_FileId(Long fileId);

    long countByEpc_CsvFile_FileId(Long fileId);
}
