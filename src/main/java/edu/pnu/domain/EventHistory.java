package edu.pnu.domain;

import java.time.LocalDateTime;

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

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"csv","epc","csvLocation","csvProduct"})
@Entity
@Table(
  name = "event_history",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_eh_file_epc_time",
      columnNames = {"file_id","epc_id","event_time"})
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
  private Csv csv;

  // ★ 유일한 '복합 FK' 한 곳: (file_id, epc_id) -> epc(file_id, epc_id)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(name = "file_id", referencedColumnName = "file_id",
                insertable = false, updatable = false, nullable = false),
    @JoinColumn(name = "epc_id",  referencedColumnName = "epc_id",
                nullable = false)
  })
  private Epc epc;

  // 위치/상품은 단일 FK로 단순화 (방법 A)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "csv_location_id", nullable = false)
  private CsvLocation csvLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private CsvProduct csvProduct; // 선택: 이벤트 시점 상품을 담고 싶을 때

  private LocalDateTime eventTime;

  private String businessStep;

  private String eventType;
  
  @Builder.Default
  private boolean anomaly = false;
}
