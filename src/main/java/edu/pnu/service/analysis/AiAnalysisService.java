package edu.pnu.service.analysis;

import edu.pnu.config.AiClientProperties;
import edu.pnu.domain.AiAnalysis;
import edu.pnu.domain.Csv;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.AiCommunicationDTO;
import edu.pnu.repository.AiAnalysisRepository;
import edu.pnu.repository.CsvRepository;
import edu.pnu.repository.EventHistoryRepository;
import edu.pnu.service.messaging.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final CsvRepository csvRepo;
    private final EventHistoryRepository eventHistoryRepo;
    private final AiAnalysisRepository aiAnalysisRepo;
    private final WebSocketService webSocketService;
    private final AiClientProperties aiClientProperties;

    private final ObjectMapper om = new ObjectMapper();

    // v1의 autoSendLatestFile 기능
    public void autoSendLatestFile() {
        log.warn("[진입] : [AiAnalysisService] AI로 DB 전송 시작");
        Long lastFileId = csvRepo.findTopByOrderByFileIdDesc()
                .map(Csv::getFileId).orElse(null);
        if (lastFileId == null) {
            log.warn("[AI] CSV 파일이 없습니다.");
            return;
        }
        log.info("[AI] 최신 파일 자동 전송: fileId={}", lastFileId);
        sendAndReceiveFromAi(lastFileId);
    }


    // ■■■■■■■■■■■■■  특정 파일 ID로 EventHistory 리스트를 DTO로 변환 ■■■■■■■■■■■■■■
    @Transactional(readOnly = true)
    public List<AiCommunicationDTO.ExportRequest> exportByFileId(Long fileId) {

        // [1] fileId null 체크를 쿼리 전에!
        if (fileId == null)
            throw new IllegalArgumentException("fileId is null");

        // [2] 쿼리 실행
        List<EventHistory> rows = eventHistoryRepo.findAllByCsv_FileIdForAiExport(fileId);

        Map<String, List<EventHistory>> byEpc = rows.stream()
                .collect(Collectors.groupingBy(e -> e.getEpc().getEpcCode(),
                        LinkedHashMap::new, Collectors.toList()));

        List<AiCommunicationDTO.ExportRequest> result = new ArrayList<>(byEpc.size());
        for (Map.Entry<String, List<EventHistory>> e : byEpc.entrySet()) {
            List<AiCommunicationDTO.EventData> events = e.getValue().stream()
                    .map(this::toEventData)
                    .toList();
            result.add(AiCommunicationDTO.ExportRequest.builder()
                    .epcCode(e.getKey())
                    .events(events)
                    .build());
        }
        return result;
    }

    //	 ■■■■■■■■■■  AI 서버에 데이터 전송 및 결과 수신 로직  ■■■■■■■■■■
    @Transactional  // 반드시 readOnly = false 또는 생략!
    public void sendAndReceiveFromAi(Long fileId) {
        Csv csv = csvRepo.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Csv not found: " + fileId));
        final String userId = csv.getMember() != null ? csv.getMember().getUserId() : null;

        notify(userId, "[AI] 이벤트 수집 시작");
        List<AiCommunicationDTO.ExportRequest> dtoList = exportByFileId(fileId);
        if (dtoList.isEmpty()) {
            notify(userId, "[AI] 전송할 이벤트 없음");
            return;
        }
        notify(userId, "[AI] 이벤트 수집 완료: EPC " + dtoList.size() + "개");

        RestTemplate rest = restTemplate(aiClientProperties.getRestConnectTimeout(),
                aiClientProperties.getRestReadTimeout());

        int total = dtoList.size();
        int savedTotal = 0;

        for (int i = 0; i < total; i += aiClientProperties.getBatchSize()) {
            int end = Math.min(i + aiClientProperties.getBatchSize(), total);
            List<AiCommunicationDTO.ExportRequest> batch = dtoList.subList(i, end);
            int batchIndex = (i / aiClientProperties.getBatchSize()) + 1;

            notify(userId, String.format("[AI] 전송: EPC %d ~ %d / %d", i + 1, end, total));

            // v1 호환 요청 바디: {"data":[ ... ]}
            String reqJson = wrapAsV1DataArray(batch);
            HttpEntity<String> req = json(reqJson);

            String respJson = callWithRetry(
                    () -> rest.postForEntity(aiClientProperties.getAiApiUrl().trim(),
                            req, String.class).getBody(), batchIndex
            );

            List<AiCommunicationDTO.ImportPayload> payloads = parseImportPayload(respJson);
            int saved = saveAiAnalyses(payloads);
            savedTotal += saved;

            notify(userId, String.format("[AI] 수신/저장: %d건 (누적 %d)", saved, savedTotal));
            sleep(aiClientProperties.getBatchDelayMs());
        }

        notify(userId, "[AI] 전체 처리 완료: 총 저장 " + savedTotal + "건");
    }

    // ===== 변환/저장 =====

    private AiCommunicationDTO.EventData toEventData(EventHistory eh) {
        return AiCommunicationDTO.EventData.builder()
                .eventId(eh.getEventId())
                .locationId(eh.getCsvLocation().getLocationId())
                .businessStep(eh.getBusinessStep())
                .eventType(eh.getEventType())
                .eventTime(eh.getEventTime())
                .build();
    }

    private List<AiCommunicationDTO.ImportPayload> parseImportPayload(String json) {
        try {
            return om.readValue(json, new TypeReference<List<AiCommunicationDTO.ImportPayload>>() {
            });
        } catch (Exception ignore) {
            try {
                AiCommunicationDTO.ImportPayload single =
                        om.readValue(json, AiCommunicationDTO.ImportPayload.class);
                return List.of(single);
            } catch (Exception e2) {
                log.error("[AI] 응답 파싱 실패: {}", json, e2);
                throw new IllegalStateException("AI 응답 파싱 실패", e2);
            }
        }
    }

    @Transactional
    protected int saveAiAnalyses(List<AiCommunicationDTO.ImportPayload> items) {
        if (items == null || items.isEmpty()) return 0;
        List<AiAnalysis> list = new ArrayList<>();

        for (AiCommunicationDTO.ImportPayload p : items) {
            if (p.getEventHistory() == null) continue;

            for (AiCommunicationDTO.AnomalyEvent a : p.getEventHistory()) {
                if (a.getEventId() == null) continue;

                EventHistory eh = eventHistoryRepo.findById(a.getEventId()).orElse(null);
                if (eh == null) continue;

                list.add(AiAnalysis.builder()
                        .eventHistory(eh)
                        .anomalyScore(a.getAnomalyScore())
                        .build());
            }
        }

        if (!list.isEmpty()) aiAnalysisRepo.saveAll(list);
        return list.size();
    }

    // ===== HTTP/재시도 유틸 =====

    private RestTemplate restTemplate(int connMs, int readMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connMs);
        f.setReadTimeout(readMs);
        return new RestTemplate(f);
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    private String wrapAsV1DataArray(List<AiCommunicationDTO.ExportRequest> batch) {
        Map<String, Object> m = new HashMap<>();
        m.put("data", batch);
        try {
            return om.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("요청 JSON 직렬화 실패", e);
        }
    }

    private <T> T callWithRetry(IoCall<T> call, int batchIndex) {
        Exception last = null;
        for (int attempt = 1; attempt <= aiClientProperties.getRetryMaxAttempts() + 1; attempt++) {
            try {
                return call.call();
            } catch (Exception e) {
                last = e;
                log.warn("[AI][배치:{}] 전송 실패 (시도 {}/{}): {}", batchIndex, attempt,
                        aiClientProperties.getRetryMaxAttempts() + 1, e.getMessage());
                if (attempt > aiClientProperties.getRetryMaxAttempts()) break;
                sleep(aiClientProperties.getRetryDelayMs());
            }
        }
        throw new RuntimeException("[AI] 재시도 초과로 전송 실패", last);
    }

    private void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void notify(String userId, String msg) {
        if (userId != null) webSocketService.sendMessage(userId, msg);
    }

    @FunctionalInterface
    private interface IoCall<T> {
        T call() throws Exception;
    }

}
