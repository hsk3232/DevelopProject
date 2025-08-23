package edu.pnu.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = "eventHistory")
@Entity
@Table(name = "beanalysis")
public class BeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "be_id")
    private Long beId;

    // 분석 대상이 되는 원본 이벤트 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private EventHistory eventHistory;

    // 백엔드가 분류한 이상 유형 (clone, tamper, fake, other 등)
    @Column(nullable = false)
    private String anomalyType;
    
    @Column(nullable = false)  // 백엔드가 분류한 이상 유형 (product, time, location 등)
    private String anomalyDetailedType;

    // 분석이 수행된 시각
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();
}
