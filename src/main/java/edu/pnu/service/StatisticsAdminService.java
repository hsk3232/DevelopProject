package edu.pnu.service;

import edu.pnu.domain.AnalysisSummary;
import edu.pnu.domain.Csv;
import edu.pnu.repository.AiAnalysisRepository;
import edu.pnu.repository.AnalysisSummaryRepository;
import edu.pnu.repository.AnalysisTripRepository;
import edu.pnu.repository.BeAnalysisRepository;
import edu.pnu.repository.CsvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAdminService {

    private final BeAnalysisService beAnalysisService;
    private final AnalysisSummaryRepository analysisSummaryRepo;
    private final BeAnalysisRepository beAnalysisRepo;
    private final AnalysisTripRepository analysisTripRepo;
    private final AiAnalysisRepository aiAnalysisRepo; // AI 분석 결과 Repository
    private final CsvRepository csvRepo;

    public void processBeAnalysis(Long fileId) {
        // 백엔드 분석의 실제 로직은 BeAnalysisService에 위임
        beAnalysisService.executeAnalysis(fileId);
    }

    public void processTripAndSummaryStatistics(Long fileId) {
        log.info("[시작] fileId={}의 최종 통계(AnalysisSummary) 집계를 시작합니다.", fileId);

        // 1. Csv 엔티티를 찾습니다. (Summary의 주인이 됨)
        Csv csv = csvRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Csv not found with id: " + fileId));

        // 2. 각 Repository에 구현된 집계 쿼리를 호출하여 KPI 값을 계산합니다.
        long totalTripCount = analysisTripRepo.countByFileId(fileId);
        long beAnomalyCount = beAnalysisRepo.countByFileId(fileId);
        long aiAnomalyCount = aiAnalysisRepo.countByFileId(fileId);
        // ... 기타 KPI 계산 쿼리 호출 ...
        // double averageLeadTime = analysisTripRepo.findAverageLeadTimeByFileId(fileId);

        // 3. 계산된 값으로 AnalysisSummary 엔티티를 생성하거나 업데이트합니다.
        AnalysisSummary summary = AnalysisSummary.builder()
                .csv(csv) // 1:1 관계 설정
                .fileId(fileId) // PK 설정
                .totalEventCount(totalTripCount) // 이름은 다르지만 Trip 총 수를 의미
                .totalErrorCount((int) beAnomalyCount) // 백엔드 탐지 이상 총 수
                .aiTotalAnomalyCount((int) aiAnomalyCount) // AI 탐지 이상 총 수
                .avgLeadTime(averageLeadTime)
                .build();
        analysisSummaryRepo.save(summary);

        log.info("[완료] fileId={}의 최종 통계 집계를 완료했습니다.", fileId);
    }


    @Async("taskExecutor")
    public CompletableFuture<Void> processBeAnalysisAsync(Long fileId) {
        processBeAnalysis(fileId);
        return CompletableFuture.completedFuture(null);
    }
}
