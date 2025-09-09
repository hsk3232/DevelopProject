package edu.pnu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@ToString(exclude = "epc")
@Entity
@Table(name = "analysistrips")
public class AnalysisTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    // 이 이동 경로의 주체가 되는 EPC (Epc의 PK를 FK로 가짐)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epc_id", nullable = false)
    private Epc epc;

    // 이동 경로 정보
    private Long fromLocationId;
    private Long toLocationId;
    private String fromScanLocation;
    private String toScanLocation;
    private String fromBusinessStep;
    private String toBusinessStep;
    private LocalDateTime fromEventTime;
    private LocalDateTime toEventTime;

    // 이 경로가 어떤 이벤트(도착점 기준)와 관련된 이상을 포함하는지 여부
    @Builder.Default
    private boolean hasAnomaly = false;

    // 원본 이벤트 ID (도착점 기준)
    private Long relatedEventId;
}
