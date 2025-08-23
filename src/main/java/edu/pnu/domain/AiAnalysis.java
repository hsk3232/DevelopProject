package edu.pnu.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "aianalysis")
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_id")
    private Long aiId;
    
    // 분석 대상이 되는 원본 이벤트 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private EventHistory eventHistory;

    // AI가 예측한 이상 확률 (예: 0.98)
    private Double anomalyScore;

    
    @CreationTimestamp
    @Column(name = "analyzed_at", updatable = false, nullable = false)
    private LocalDateTime analyzedAt;

}
