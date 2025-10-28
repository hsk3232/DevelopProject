package edu.pnu.service.analysis.be.detector;

import edu.pnu.domain.BeAnalysis;
import edu.pnu.domain.EventHistory;
import edu.pnu.service.analysis.be.api.BeDetector;
import edu.pnu.service.analysis.be.support.AssetCache;
import edu.pnu.service.analysis.be.support.EpcSerialValidatorService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectClonesAndFake implements BeDetector {

    // 스프링이 EpcSerialValidatorService Bean을 자동으로 주입해 줍니다.
    private final EpcSerialValidatorService serialValidator;

    // v1 FindAnomalyComponet의 'tamper'와 'fake'를 판별하던 로직
    @Override
    public List<BeAnalysis> detect(Map<String, List<EventHistory>> eventsByEpc,
                                   Set<String> alreadyDetectedEpcIds,
                                   AssetCache assetCache) {

        List<BeAnalysis> results = new ArrayList<>();
        Set<String> knownLots = serialValidator.getAllKnownLots();

        for (Map.Entry<String, List<EventHistory>> entry : eventsByEpc.entrySet()) {
            String epcCode = entry.getKey();
            if (alreadyDetectedEpcIds.contains(epcCode)) {
                continue;
            }

            List<EventHistory> events = entry.getValue();
            if (events.isEmpty()) continue;

            EventHistory representativeEvent = events.get(0);
            String productCode = representativeEvent.getCsvProduct().getEpcProduct();
            String companyCode = representativeEvent.getCsvProduct().getEpcCompany();
            String productName = representativeEvent.getCsvProduct().getProductName();
            String lot = representativeEvent.getEpc().getEpcLot();

            boolean isKnownProduct = assetCache.getKnownProductCodes().contains(productCode);
            boolean isKnownCompany = assetCache.getKnownCompanyCodes().contains(companyCode);
            boolean isKnownName = assetCache.getKnownProductNames().contains(productName);

            // v1 로직 (1): 완전 일치 여부 확인
            boolean isFullMatch = isKnownProduct && isKnownCompany && isKnownName;

            // [A] 기준 정보에 완전 일치하는 제품이 없는 경우 (tamper 또는 fake 후보)
            if (!isFullMatch) {
                // v1 로직 (2): 부분 일치 또는 Lot/Serial 가능성 확인
                boolean isPartialMatch = isKnownProduct || isKnownCompany || isKnownName;
                boolean isKnownLot = knownLots.contains(lot);
                boolean isPotentialSerial = false;
                try {
                    int serial = Integer.parseInt(representativeEvent.getEpc().getEpcSerial());
                    isPotentialSerial = serialValidator.isPotentiallyValidSerial(serial);
                } catch (NumberFormatException e) {
                    // 시리얼이 숫자가 아니면 가능성 없음
                }

                String anomalyType;
                String detail;

                // 일부라도 정보가 걸리면 'Tamper'
                if (isPartialMatch || isKnownLot || isPotentialSerial) {
                    anomalyType = "Tamper";
                    detail = "Partial Product Info Mismatch";
                }
                // 어떤 정보에도 걸리지 않으면 'Fake'
                else {
                    anomalyType = "Fake";
                    detail = "Unknown Product";
                }

                // 해당 EPC의 모든 이벤트에 대해 동일한 이상 유형을 기록
                for (EventHistory event : events) {
                    results.add(createAnomaly(event, anomalyType, detail));
                }
                alreadyDetectedEpcIds.add(epcCode);
            }
            // [B] 기준 정보와 완전 일치하는 경우 (v1의 'other' 또는 'tamper(규칙위반)' 후보)
            else {
                // v1 로직 (3): 생산 공장 기반 lot/serial 규칙 검사
                String factory = serialValidator.extractFactoryFromName(representativeEvent.getHubType());
                String lotNum = representativeEvent.getEpc().getEpcLot();

                try {
                    int serialNum = Integer.parseInt(representativeEvent.getEpc().getEpcSerial());
                    // 공장, Lot, 시리얼 번호가 모두 규칙에 맞는지 정확히 검사
                    if (!serialValidator.isValid(factory, lotNum, serialNum)) {
                        for (EventHistory event : events) {
                            results.add(createAnomaly(event, "Tamper", "Serial Rule Violation"));
                        }
                        alreadyDetectedEpcIds.add(epcCode);
                    }
                    // 규칙에 맞으면 정상으로 간주, 'other' 유형은 AI의 역할이므로 BE에서는 판정하지 않음.

                } catch (NumberFormatException e) {
                    // 시리얼이 숫자가 아닌 경우는 규칙 위반이므로 'Tamper'
                    for (EventHistory event : events) {
                        results.add(createAnomaly(event, "Tamper", "Invalid Serial Format"));
                    }
                    alreadyDetectedEpcIds.add(epcCode);
                }
            }
        }
        return results;
    }

    // BeAnalysis 객체를 생성하고 필요한 값(event, type, detail, 생성 시간)을 설정하여 반환
    // 코드 중복을 피함
    private BeAnalysis createAnomaly(EventHistory event, String type, String detail) {
        BeAnalysis anomaly = new BeAnalysis();
        anomaly.setEventHistory(event);
        anomaly.setAnomalyType(type);
        anomaly.setAnomalyDetailedType(detail);
        anomaly.setAnalyzedAt(java.time.LocalDateTime.now());
        return anomaly;
    }

    @Override
    public int getPriority() {
        return 2; // 가장 낮은 우선순위
    }
}
