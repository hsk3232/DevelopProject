package edu.pnu.service.analysis;

import edu.pnu.config.AiClientProperties;
import edu.pnu.repository.AiAnalysisRepository;
import edu.pnu.repository.CsvRepository;
import edu.pnu.repository.EventHistoryRepository;
import edu.pnu.service.messaging.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final CsvRepository csvRepo;
    private final EventHistoryRepository eventHistoryRepo;
    private final AiAnalysisRepository aiAnalysisRepo;
    private final WebSocketService webSocketService;
    private final AiClientProperties aiClientProperties;

    public void sendAndReceiveFromAi(Long fileId) {
        // TODO Auto-generated method stub

    }
    
}
