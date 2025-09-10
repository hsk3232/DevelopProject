package edu.pnu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsAdminService {

    private final List<StatisticsInterface> statisticsInterfaces;

    public void runAllStatsAfterBeAi(Long fileId, String userId, WebSocketService ws) {
        statisticsInterfaces.stream()
                .sorted(Comparator.comparingInt(StatisticsInterface::getOrder))
                .forEach(task -> {
                    ws.sendMessage(userId, "[진행중] " + task.getProcessorName() + "...");
                    task.process(fileId);
                    ws.sendMessage(userId, "[완료] " + task.getProcessorName());
                });
    }
}
