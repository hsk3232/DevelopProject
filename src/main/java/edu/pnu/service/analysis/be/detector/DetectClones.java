package edu.pnu.service.analysis.be.detector;

import edu.pnu.domain.CsvRoute;
import edu.pnu.domain.BeAnalysis;
import edu.pnu.domain.EventHistory;
import edu.pnu.service.analysis.be.api.BeDetector;
import edu.pnu.service.analysis.be.support.AssetCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectClones implements BeDetector {



    @Override
    public List<BeAnalysis> detect(Map<String, List<EventHistory>> eventsByEpc,
                                   Map<String, List<CsvRoute>> tripsByEpc,
                                   Set<String> alreadyDetectedEpcIds,
                                   AssetCache assetCache) {

        List<BeAnalysis> results = new ArrayList<>();
        Set<String> validRoutes = assetCache.getValidRoutes();
        if (tripsByEpc == null || tripsByEpc.isEmpty()) {
            log.info("[DetectRouteViolation] tripsByEpc가 비어있습니다. 검사 종료.");
            return results;
        }

        // EPC별 분석
        for (Map.Entry<String, List<CsvRoute>> entry : tripsByEpc.entrySet()) {
            String epcCode = entry.getKey();
            if (epcCode == null) continue;

            if (alreadyDetectedEpcIds.contains(epcCode)) {
                // 이미 다른 탐지기에서 확정된 EPC는 건너뜀
                continue;
            }

            List<CsvRoute> trips = entry.getValue();
            if (trips == null || trips.isEmpty()) continue;

            // 중요: trips가 시간순으로 정렬되어 있지 않을 수 있으므로, fromEventTime을 기준으로 null-safe 정렬
            trips.sort(Comparator.comparing(CsvRoute::getFromEventTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            for (CsvRoute trip : trips) {
                if (trip == null) continue;

                Long fromLocId = trip.getFromLocationId();
                Long toLocId = trip.getToLocationId();

                // fromEntity/to Location ID가 없으면 검사 불가 -> 로깅 후 건너뜀
                if (fromLocId == null || toLocId == null) {
                    log.debug("[DetectRouteViolation] trip에 위치정보 없음: epc={}, tripId(relatedEventId)={}, fromLoc={}, toLoc={}",
                            epcCode, trip.getRelatedEventId(), fromLocId, toLocId);
                    continue;
                }

                // 문자열 포맷 일치가 중요 (공백 등 주의)
                String currentRoute = fromLocId + "->" + toLocId;

                if (!validRoutes.contains(currentRoute)) {
                    // 연관 EventHistory를 찾아 이상 기록용 이벤트를 확보
                    EventHistory relatedEvent = findEventById(eventsByEpc.get(epcCode), trip.getRelatedEventId());

                    if (relatedEvent == null) {
                        // EventHistory가 없으면 이상을 저장할 수 없음 — 운영 로그 남김, 검사 계속 (기록하지 않음)
                        log.warn("[DetectRouteViolation] 관련 EventHistory를 찾을 수 없어 경로위반 기록을 생략합니다. epc={}, relatedEventId={}, route={}",
                                epcCode, trip.getRelatedEventId(), currentRoute);
                        // 관련 이벤트가 없으므로 alreadyDetected로 등록하지 않음
                        continue;
                    }

                    BeAnalysis anomaly = createAnomaly(relatedEvent, "Tamper", "Route Violation");
                    if (anomaly != null) {
                        results.add(anomaly);
                        alreadyDetectedEpcIds.add(epcCode); // 이상 확정시만 배제 목록에 추가
                        log.info("[DetectRouteViolation] 경로 위반 탐지: epc={}, route={}, eventId={}", epcCode, currentRoute, relatedEvent.getEventId());
                        break; // EPC당 하나만 기록하고 다음 EPC로
                    } else {
                        log.warn("[DetectRouteViolation] anomaly 생성 실패 (null) epc={}, relatedEventId={}", epcCode, trip.getRelatedEventId());
                    }
                }
            }
        }
        return results;
    }

    // EventHistory 조회 헬퍼 (null-safe)
    private EventHistory findEventById(List<EventHistory> events, Long eventId) {
        if (events == null || eventId == null) return null;
        return events.stream().filter(e -> eventId.equals(e.getEventId())).findFirst().orElse(null);
    }

    // BeAnalysis 빌더 (event null이면 null 반환)
    private BeAnalysis createAnomaly(EventHistory event, String type, String detail) {
        if (event == null) return null;
        return BeAnalysis.builder()
                .eventHistory(event)
                .anomalyType(type)
                .anomalyDetailedType(detail)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public int getPriority() {
        return 1; // Clone 다음 우선순위
    }
}
