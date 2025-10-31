package edu.pnu.service.analysis.be.detector;

import edu.pnu.domain.CsvRoute;
import edu.pnu.domain.BeAnalysis;
import edu.pnu.domain.EventHistory;
import edu.pnu.service.analysis.be.api.BeDetector;
import edu.pnu.service.analysis.be.support.AssetCache;
import edu.pnu.service.analysis.be.support.EpcSerialValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectProductMismatch implements BeDetector {

    private final EpcSerialValidatorService serialValidator;

    @Override
    public List<BeAnalysis> detect(
            Map<String, List<EventHistory>> eventsByEpc,
            Map<String, List<CsvRoute>> tripsByEpc,
            Set<String> alreadyDetectedEpcIds,
            AssetCache assetCache) {

        List<BeAnalysis> results = new ArrayList<>();
        Set<String> knownLots = serialValidator.getAllKnownLots();

        for (Map.Entry<String, List<EventHistory>> entry : eventsByEpc.entrySet()) {
            String epcCode = entry.getKey();
            if (epcCode == null) continue;
            if (alreadyDetectedEpcIds.contains(epcCode)) {
                continue;
            }

            List<EventHistory> events = entry.getValue();
            if (events == null || events.isEmpty()) continue;

            // 시간순 정렬 (null-safe) — representativeEvent 결정의 신뢰성 확보
            List<EventHistory> sorted = events.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(EventHistory::getEventTime,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (sorted.isEmpty()) continue;

            EventHistory representativeEvent = events.get(0);

            if (representativeEvent.getCsvProduct() == null || representativeEvent.getEpc() == null) {
                log.warn("[DetectProductMismatch] 대표 이벤트에 제품/epc 정보 없음: epc={}, repEventId={}",
                        epcCode, representativeEvent.getEventId());
                continue;
            }

            String productCode = representativeEvent.getCsvProduct().getEpcProduct();
            String companyCode = representativeEvent.getCsvProduct().getEpcCompany();
            String productName = representativeEvent.getCsvProduct().getProductName();

            boolean isKnownProduct = assetCache.getKnownProductCodes().contains(productCode);
            boolean isKnownCompany = assetCache.getKnownCompanyCodes().contains(companyCode);
            boolean isKnownName = assetCache.getKnownProductNames().contains(productName);

            boolean isFullMatch = isKnownProduct && isKnownCompany && isKnownName;

            if (!isFullMatch) {
                boolean isPartialMatch = isKnownProduct || isKnownCompany || isKnownName;
                // 타입 안전성: epcLot가 null이거나 non-string일 경우 대비
                String epcLotVal = String.valueOf(representativeEvent.getEpc().getEpcLot());
                boolean isKnownLot = epcLotVal != null && knownLots.contains(epcLotVal);

                boolean isPotentialSerial = false;
                String epcSerialStr = String.valueOf(representativeEvent.getEpc().getEpcSerial());

                try {
                    isPotentialSerial = serialValidator.isPotentiallyValidSerial(Integer.parseInt(epcSerialStr));
                } catch (NumberFormatException nfe) {
                    // 유효 숫자가 아니라면 후보 아님 — 로깅
                    log.debug("[DetectProductMismatch] serial parse 실패: epc={}, serial={}", epcCode, epcSerialStr);
                } catch (Exception e) {
                    log.warn("[DetectProductMismatch] serial 검증 중 예외: epc={}, serial={}, ex={}", epcCode, epcSerialStr, e.toString());
                }

                String anomalyType = (isPartialMatch || isKnownLot || isPotentialSerial) ? "Tamper" : "Fake";
                String detail = (isPartialMatch || isKnownLot || isPotentialSerial) ? "Partial Product Info Mismatch" : "Unknown Product";

                // 이벤트별로 anomaly 생성 — createAnomaly null 체크 후 추가
                for (EventHistory event : events) {
                    results.add(createAnomaly(event, anomalyType, detail));
                }
                // 이상이 기록된 경우에만 alreadyDetected 추가
                if (!results.isEmpty()) {
                    alreadyDetectedEpcIds.add(epcCode);
                }
            }
        }
        return results;
    }

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
        return 2; // 가장 낮은 우선순위
    }
}
