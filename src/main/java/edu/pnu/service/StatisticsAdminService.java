package edu.pnu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAdminService {

    public void processBeAnalysis(Long fileId) {
        // TODO Auto-generated method stub

    }

    public void processTripAndSummaryStatistics(Long fileId) {
        // TODO Auto-generated method stub

    }

    @Async("taskExecutor")
    public CompletableFuture<Void> processBeAnalysisAsync(Long fileId) {
        processBeAnalysis(fileId);
        return CompletableFuture.completedFuture(null);
    }
}
