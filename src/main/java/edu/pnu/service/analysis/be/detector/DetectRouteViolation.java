package edu.pnu.service.analysis.be.detector;

import edu.pnu.domain.CsvRoute;
import edu.pnu.domain.BeAnalysis;
import edu.pnu.domain.EventHistory;
import edu.pnu.service.analysis.be.api.BeDetector;
import edu.pnu.service.analysis.be.support.AssetCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectRouteViolation implements BeDetector {

    @Override
    public List<BeAnalysis> detect(Map<String, List<EventHistory>> eventsByEpc,
                                   Map<String, List<CsvRoute>> tripsByEpc,
                                   Set<String> alreadyDetectedEpcIds,
                                   AssetCache assetCache) {
        List<BeAnalysis> results = new ArrayList<>();
        Set<String> validRoutes = assetCache.getValidRoutes();

        for (Map.Entry<String, List<EventHistory>> entry : eventsByEpc.entrySet()) {
            String epcCode = entry.getKey();
            // 이미 다른 높은 우선순위의 탐지기에 의해 감지되었다면 건너뜁니다.
            if (alreadyDetectedEpcIds.contains(epcCode)) {
                continue;
            }

            List<EventHistory> events = entry.getValue();

            // EPC별 이벤트는 시간순 정렬
            events.sort(Comparator.comparing(EventHistory::getEventTime, Comparator.nullsLast(Comparator.naturalOrder())));
            // 이벤트가 2개 이상이어야 경로가 생성됩니다.
            for (int i = 0; i < events.size() - 1; i++) {
                EventHistory fromEvent = events.get(i);
                EventHistory toEvent = events.get(i + 1);

                if (fromEvent == null || toEvent == null) continue;

                // csvLocation 혹은 locationId가 null이면 그 쌍은 검사 불가 -> 건너뜀
                if (fromEvent.getCsvLocation() == null || toEvent.getCsvLocation() == null) {
                    log.warn("[DetectRouteViolation] missing csvLocation: epc={}, fromEventId={}, toEventId={}",
                            epcCode,
                            fromEvent.getEventId(),
                            toEvent.getEventId());
                    continue;
                }

                Long fromLocId = fromEvent.getCsvLocation().getCsvLocationId();
                Long toLocId = toEvent.getCsvLocation().getCsvLocationId();

                if (fromLocId == null || toLocId == null) {
                    log.warn("[DetectRouteViolation] missing locationId: epc={}, fromEventId={}, toEventId={}",
                            epcCode, fromEvent.getEventId(), toEvent.getEventId());
                    continue;
                }

                // Long 타입을 String 포맷으로 결합 (예: "1->2")
                String currentRoute = fromLocId + "->" + toLocId;

                if (!validRoutes.contains(currentRoute)) {
                    // 도착점(toEvent) 기준으로 이상을 기록합니다.
                    BeAnalysis anomaly = new BeAnalysis();
                    anomaly.setEventHistory(toEvent);
                    anomaly.setAnomalyType("Tamper");
                    anomaly.setAnomalyDetailedType("Route");
                    anomaly.setAnalyzedAt(java.time.LocalDateTime.now());
                    results.add(anomaly);

                    // 해당 EPC는 이상으로 확정되었으므로, 더 낮은 순위의 탐지기에서는 무시됩니다.
                    alreadyDetectedEpcIds.add(epcCode);
                    // 한 EPC에서 경로 위반이 한 번이라도 발견되면 더 이상 검사하지 않습니다.
                    break;
                }
            }
        }
        return results;
    }

    @Override
    public int getPriority() {
        return 1; // Clone 다음으로 높은 우선순위
    }

}
