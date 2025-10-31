package edu.pnu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"csvFile", "epc", "csvLocation", "csvProduct"})
@Entity

@Table(name = "event_history",
        // 비즈니스 규칙: 특정 파일의, 특정 EPC는, 특정 시간, 특정 위치, 특정 이벤트 타입을 중복으로 가질 수 없음.
        // 이 제약조건은 event_id를 제외한 완벽하게 동일한 이벤트 데이터의 중복 삽입을 방지
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_prevent_duplicate_event_history",
                        columnNames = {
                                "file_id", "epc_id", "csv_location_id", "csv_product_id",
                                "event_time", "business_step", "event_type"
                        }
                )
        },
        indexes = {
                @Index(name = "ix_eh_epc_time", columnList = "file_id,epc_id,event_time"),
                @Index(name = "ix_eh_loc_time", columnList = "file_id,csv_location_id,event_time")
        }
)
public class EventHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    // 파일 스코프 기준 키 (필수)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private CsvFile csvFile;

    // ★ 유일한 '복합 FK' 한 곳: (file_id, epc_id) -> epc(file_id, epc_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "file_id", referencedColumnName = "file_id", nullable = false),
            @JoinColumn(name = "epc_id", referencedColumnName = "epc_id", nullable = false)
    })
    private Epc epc;

    // 위치/상품은 단일 FK로 단순화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "csv_location_id", nullable = false)
    private CsvLocation csvLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "csv_product_id", nullable = false)
    private CsvProduct csvProduct;

    @Column(name = "hub_type")
    private String hubType;

    @Column(name = "business_step", nullable = false)
    private String businessStep;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "business_original")
    private String businessOriginal;


}
