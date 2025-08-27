package edu.pnu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class AnalysisPipelineService {
    private final DataShareService dataShareService;
    private final StatisticsAdminService statisticsAdminService;
    private final WebSocketService webSocketService;
    private final Executor taskExecutor;

    @Async("taskExecutor")
    public void runAnalysisPipeline(Long fileId, String userId) {
        webSocketService.sendMessage(userId, "AI 분석과 백엔드 분석을 동시에 시작합니다...");

        CompletableFuture<Void> ai = CompletableFuture.runAsync(() -> {
            webSocketService.sendMessage(userId, "[진행중] AI 서버 연동 및 분석...");
            dataShareService.sendAndReceiveFromAi(fileId);
            webSocketService.sendMessage(userId, "[완료] AI 분석 완료.");
        }, taskExecutor);

        CompletableFuture<Void> be = CompletableFuture.runAsync(() -> {
            webSocketService.sendMessage(userId, "[진행중] 백엔드 규칙 기반 분석...");
            statisticsAdminService.processBeAnalysis(fileId);
            webSocketService.sendMessage(userId, "[완료] 백엔드 분석 완료.");
        }, taskExecutor);

        CompletableFuture.allOf(ai, be).join();

        webSocketService.sendMessage(userId, "모든 분석 작업 완료. 최종 통계를 생성합니다.");
        statisticsAdminService.processTripAndSummaryStatistics(fileId);
        webSocketService.sendMessage(userId, "모든 처리 과정이 성공적으로 완료되었습니다!");
    }
}


