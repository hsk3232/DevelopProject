package edu.pnu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataShareService {

    public void sendAndReceiveFromAi(Long fileId) {
        // TODO Auto-generated method stub

    }

    @Async("taskExecutor")
    public CompletableFuture<Void> sendAndReceiveFromAiAsync(Long fileId) {
        sendAndReceiveFromAi(fileId);
        return CompletableFuture.completedFuture(null);
    }
}
