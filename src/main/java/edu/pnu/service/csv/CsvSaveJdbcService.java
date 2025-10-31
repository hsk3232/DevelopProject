package edu.pnu.service.csv;

import edu.pnu.domain.CsvLocation;
import edu.pnu.domain.CsvProduct;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveJdbcService {

    // JdbcTemplate을 사용해 엔티티들을 고속으로 배치 삽입
    private final JdbcTemplate jdbcTemplate;

    // CsvLocation 배치 삽입
    public void saveCsvLocations(List<CsvLocation> locations) {
        if (locations.isEmpty())
            return;

        String sql = "INSERT INTO csvlocation "
                + "(file_id, location_id, scan_location, operator_id, device_id) "
                + "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                CsvLocation l = locations.get(i);
                ps.setLong(1, l.getCsvFile().getFileId());
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
        log.debug("[CsvSaveJdbcService] [성공] :  [saveCsvLocations] Location batch insert 완료! 저장 건수: {}", locations.size());
    }

    // CsvProduct 배치 삽입
    public void saveCsvProducts(List<CsvProduct> products) {
        if (products.isEmpty()) return;

        String sql = "INSERT INTO csvproduct "
                + "(file_id, epc_product, epc_company, product_name) "
                + "VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                CsvProduct p = products.get(i);
                ps.setLong(1, p.getCsv().getFileId());
                ps.setString(2, p.getEpcProduct());
                ps.setString(3, p.getEpcCompany());
                ps.setString(4, p.getProductName());
            }

            @Override
            public int getBatchSize() {
                return products.size();
            }
        });

        log.debug("[CsvSaveJdbcService] [성공] : [saveCsvProducts] Product batch insert 완료! 저장 건수: {}", products.size());
    }

    // Epc 배치 삽입
    public void saveEpcs(List<Epc> epcs) {
        if (epcs.isEmpty()) return;
        String sql = "INSERT INTO epc "
                + "(file_id, epc_code, epc_header, epc_lot, epc_serial, manufacture_date, expiry_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                Epc e = epcs.get(i);
                ps.setLong(1, e.getCsvFile().getFileId());
                ps.setString(2, e.getEpcCode());
                ps.setString(3, e.getEpcHeader());
                ps.setString(4, e.getEpcLot());
                ps.setString(5, e.getEpcSerial());
                ps.setObject(6, e.getManufactureDate() != null ? Timestamp.valueOf(e.getManufactureDate()) : null, Types.TIMESTAMP);
                ps.setObject(7, e.getExpiryDate() != null ? java.sql.Date.valueOf(e.getExpiryDate()) : null, Types.DATE);
            }

            @Override
            public int getBatchSize() {
                return epcs.size();
            }
        });
        log.debug("[CsvSaveJdbcService] [성공] : [saveEpcs] Epc batch insert 완료! 저장 건수: {}", epcs.size());
    }

    // EventHistory 배치 삽입
    public void saveEventHistories(List<EventHistory> events) {
        if (events.isEmpty()) return;

        String sql = "INSERT INTO eventhistory " +
                "(file_id, epc_id, csv_location_id, product_id, " +
                "event_time, business_step, event_type, hub_type, business_original) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EventHistory eh = events.get(i);
                ps.setLong(1, eh.getCsvFile().getFileId());
                ps.setLong(2, eh.getEpc().getEpcId()); // epc_code 대신 epc_id
                ps.setLong(3, eh.getCsvLocation().getLocationId()); // location_id 대신 csv_location_id
                ps.setLong(4, eh.getCsvProduct().getProductId()); // 새로 추가된 product_id
                ps.setTimestamp(5, Timestamp.valueOf(eh.getEventTime()));
                ps.setString(6, eh.getBusinessStep());
                ps.setString(7, eh.getEventType());
                ps.setString(8, eh.getHubType());
                ps.setString(9, eh.getBusinessOriginal());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });
        log.debug("[CsvSaveBatchService] [성공] : [saveEventHistories] EventHistory batch insert 완료! 저장 건수: {}, ID 설정 완료", events.size());
    }

}
