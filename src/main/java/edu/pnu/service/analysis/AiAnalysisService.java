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
import java.util.concurrent.Callable;
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

    // JSON 직/역직렬화를 위한 Jackson 매퍼 (요청/응답 변환에 사용)
    private final ObjectMapper om = new ObjectMapper();

    //  ■■■■■■■■■■■■■  [엔트리] 최신 CSV 파일을 찾아 AI 파이프라인을 수행  ■■■■■■■■■■■■■
    //  - 가장 최근 fileId를 조회하여 sendAndReceiveFromAi(fileId) 호출
    //  - 파일이 없으면 조용히 종료
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


    //  ■■■■■■■■■■■■■  [Export] 특정 fileId의 EventHistory → AI 전송용 DTO로 변환  ■■■■■■■■■■■■■
    //  - DB에서 fileId에 해당하는 이벤트들을 조회
    //  - EPC 코드별로 그룹핑하여 AiCommunicationDTO.ExportRequest 리스트로 변환
    //  - EventHistory → EventData 매핑은 toEventData(eh) 담당
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

    //	 ■■■■■■■■■■  [전송/수신] AI 서버에 데이터 전송 및 결과 수신/저장 메인 로직  ■■■■■■■■■■
    //  흐름:
    //   1) 업로더 userId 조회 → 진행상황 WebSocket 통지 채널 확보
    //   2) exportByFileId로 EPC 단위 DTO 구축
    //   3) 배치 사이즈로 분할하여 {"data":[...]} 형식으로 POST
    //   4) 응답(JSON)을 parseImportPayload로 파싱(List<ImportPayload> 또는 단건)
    //   5) saveAiAnalyses로 AiAnalysis 엔티티 저장 (EventHistory:AiAnalysis = 1:1)
    //   6) 배치 간 딜레이/재시도는 설정 값(aiClientProperties) 사용
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

    //  ■■■■■■■■■■■■■  [매핑] EventHistory → AI 전송 이벤트 DTO(EventData) 변환  ■■■■■■■■■■■■■
    private AiCommunicationDTO.EventData toEventData(EventHistory eh) {
        return AiCommunicationDTO.EventData.builder()
                .eventId(eh.getEventId())
                .locationId(eh.getCsvLocation().getLocationId())
                .businessStep(eh.getBusinessStep())
                .eventType(eh.getEventType())
                .eventTime(eh.getEventTime())
                .build();
    }

    //  ■■■■■■■■■■■■■  [파싱] AI 응답(JSON) → ImportPayload(List or 단건) 변환  ■■■■■■■■■■■■■
    //  - 1순위: List<ImportPayload> 파싱 시도
    //  - 실패 시: 단일 ImportPayload 파싱 재시도
    //  - 둘 다 실패하면 예외
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

    //  ■■■■■■■■■■■■■  [저장] ImportPayload → AiAnalysis 엔티티(점수 기반) 일괄 저장  ■■■■■■■■■■■■■
    //  - payloads의 eventId마다 EventHistory 조회 → AiAnalysis(event, anomalyScore) 생성
    //  - list가 비어있지 않으면 saveAll
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


    //  ■■■■■■■■■■■■■  [유틸] RestTemplate 생성 (타임아웃 반영)  ■■■■■■■■■■■■■
    private RestTemplate restTemplate(int connMs, int readMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connMs);
        f.setReadTimeout(readMs);
        return new RestTemplate(f);
    }

    //  ■■■■■■■■■■■■■  [유틸] JSON Content-Type 요청 엔티티 생성  ■■■■■■■■■■■■■
    private HttpEntity<String> json(String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    //  ■■■■■■■■■■■■■  [유틸] v1 형식 유지: {"data":[ ... ]} 로 요청 바디 생성  ■■■■■■■■■■■■■
    private String wrapAsV1DataArray(List<AiCommunicationDTO.ExportRequest> batch) {
        Map<String, Object> m = new HashMap<>();
        m.put("data", batch);
        try {
            return om.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("요청 JSON 직렬화 실패", e);
        }
    }

    //  ■■■■■■■■■■■■■  [유틸] 재시도 래퍼: 네트워크/IO 호출 실패 시 n회 재시도  ■■■■■■■■■■■■■
    //  - 실패 로그에 배치 인덱스/시도 회수 출력
    //  - 재시도 한도 초과 시 마지막 예외를 감싼 RuntimeException 던짐
    private <T> T callWithRetry(Callable<T> call, int batchIndex) {
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

    //  ■■■■■■■■■■■■■  [유틸] 안전한 sleep (Interrupted 상태 복구)  ■■■■■■■■■■■■■
    private void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    //  ■■■■■■■■■■■■■  [유틸] 개인 채널로 WebSocket 알림 전송 (userId 없으면 무시)  ■■■■■■■■■■■■■
    private void notify(String userId, String msg) {
        if (userId != null) webSocketService.sendMessage(userId, msg);
    }


}
