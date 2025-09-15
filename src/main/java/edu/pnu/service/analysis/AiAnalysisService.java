package edu.pnu.service.analysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    public void sendAndReceiveFromAi(Long fileId) {
        // TODO Auto-generated method stub

    }

    @Async("taskExecutor")
    public CompletableFuture<Void> sendAndReceiveFromAiAsync(Long fileId) {
        sendAndReceiveFromAi(fileId);
        return CompletableFuture.completedFuture(null);
    }
}
