package edu.pnu.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import edu.pnu.domain.CsvLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveBatchService {

	// JdbcTemplate을 사용해 엔티티들을 고속으로 배치 삽입
	private final JdbcTemplate jdbcTemplate;

	// CsvLocation 배치 삽입
	public void saveCsvLocation(List<CsvLocation> locations) {
		if (locations.isEmpty())
			return;

		String sql = "INSERT csvlocation (file_id, location_id, scan_location, operator_id, device_id) VALUES (?, ?, ?, ?, ?)";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				CsvLocation l = locations.get(i);
				ps.setLong(1, l.getCsv().getFileId());
				ps.setLong(2, l.getLocationId());
				ps.setString(3, l.getScanLocation());
				ps.setObject(4, l.getOperatorId(), Types.BIGINT);
				ps.setObject(5, l.getDeviceId(), Types.BIGINT);
			}

			@Override
			public int getBatchSize() {
				return locations.size();
			}
		});
		log.debug("[성공] : [CsvSaveBatchService] Location batch insert 완료! 저장 건수: {}", locations.size());
	}
	
	// CsvProduct 배치 삽입

}
