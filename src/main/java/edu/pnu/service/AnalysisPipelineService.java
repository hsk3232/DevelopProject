package edu.pnu.service;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisPipelineService {

    private final AiAnalysisService aiAnalysisService;
    private final BeAnalysisService beAnalysisService;
    private final StatisticsAdminService statisticsAdminService;
    private final WebSocketService webSocketService;
    private final Executor taskExecutor;

    @Async("taskExecutor")
    public void runAnalysisPipeline(Long fileId, String userId) {
        webSocketService.sendMessage(userId, "AI 분석과 백엔드 분석을 동시에 시작");

        CompletableFuture<Void> ai = CompletableFuture.runAsync(() -> {
            webSocketService.sendMessage(userId, "[진행중] AI 서버 연동 및 분석");
            aiAnalysisService.sendAndReceiveFromAi(fileId);
            webSocketService.sendMessage(userId, "[완료] AI 분석 완료.");
        }, taskExecutor);

        CompletableFuture<Void> be = CompletableFuture.runAsync(() -> {
            webSocketService.sendMessage(userId, "[진행중] BE 규칙 기반 분석");
            beAnalysisService.executeAnalysis(fileId);
            webSocketService.sendMessage(userId, "[완료] BE 분석 완료");
        }, taskExecutor);

        CompletableFuture.allOf(ai, be).join();

        webSocketService.sendMessage(userId, "모든 분석 작업 완료. 후속 집계를 실행");
        statisticsAdminService.runAllStatsAfterBeAi(fileId, userId, webSocketService); // v1 스타일
        webSocketService.sendMessage(userId, "모든 처리 과정 성공적으로 완료!");
    }
}


