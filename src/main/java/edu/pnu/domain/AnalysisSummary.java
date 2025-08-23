package edu.pnu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
@Table(name="analysissummary")
public class AnalysisSummary  {
	
	@Id
	@Column(name = "file_id") // summaryId를 써도 됨. 단, @MapsId("summaryId") 로 써야함.
	private Long fileId;
	
	// Csv 엔티티와 1:1 관계를 맺고, Csv의 PK를 자신의 PK로 사용 (성능 최적화)
	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId // Csv의 id를 이 엔티티의 id로 매핑
	@JoinColumn(name="file_id")
	private Csv csv;
	
	
//	 ■■■■■■■■■■■■ [ KPI 통계 정보 ] ■■■■■■■■■■■■
	// 기존 KPIAnalysis 역할
	private long totalEventCount; // totalTripCount -> 더 명확한 이름으로 변경
    private int uniqueProductCount;
    private long codeCount;
    private double salesRate;
    private double dispatchRate;
    private double avgLeadTime;
	
	

//	 ■■■■■■■■■■■■ [ BE 분석 통계 정보 ] ■■■■■■■■■■■■
    // 기존 FileAnomalyStats 역할. 2가지로 확대
	@Builder.Default
	private int totalErrorCount = 0;
	
	@Builder.Default
	private int fakeCount = 0;
	
	@Builder.Default
	private int tamperCount = 0;
	
	@Builder.Default
	private int cloneCount = 0;
	
	
	
//	 ■■■■■■■■■■■■ [ AI 분석 통계 정보 ] ■■■■■■■■■■■■
   // 기존 FileAnomalyStats 역할. 2가지로 확대

	@Builder.Default
	private int aiTotalAnomalyCount= 0;
   
	// AI는 유형 분류가 아닌 점수 기반이므로, 추가적인 통계가 들어갈 수 있음
	@Builder.Default
    private double averageAnomalyScore= 0;
	
	@Builder.Default
    private int highConfidenceAnomalyCount= 0; // 예: 95% 이상 확신하는 이상 건수

}