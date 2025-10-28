package edu.pnu.service.analysis.be.support;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Slf4j
@Component // 스프링 Bean으로 등록하여 싱글턴으로 관리
public class EpcSerialValidatorService {

    // 내부 클래스, 외부에서 접근할 필요 없으므로 private static으로 선언
    private static class SerialRange {
        int start;
        int end;

        SerialRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        boolean contains(int value) {
            return value >= start && value <= end;
        }
    }

    // factory → (lot → serial range) 맵
    private final Map<String, Map<String, SerialRange>> factoryLotSerialRanges = new HashMap<>();

    public EpcSerialValidatorService() {
        // v1의 초기화 로직을 그대로 사용
        factoryLotSerialRanges.put("화성", initFactoryLotsWithResets(50001, 26, 2000, 16));
        factoryLotSerialRanges.put("인천", initFactoryLotsWithResets(10001, 51, 2000, 16));
        factoryLotSerialRanges.put("구미", initFactoryLotsWithResets(150001, 11, 2000, 16));
        factoryLotSerialRanges.put("양산", initFactoryLotsWithResets(100001, 32, 2000, 16));
        log.info("EpcSerialValidatorService 초기화 완료. 4개 공장 규칙 로드.");
    }

    private Map<String, SerialRange> initFactoryLotsWithResets(int startLot, int lotCount, int countPerLot, int resetInterval) {
        Map<String, SerialRange> lotMap = new LinkedHashMap<>();
        int serialStart = 1;
        for (int i = 0; i < lotCount; i++) {
            int lot = startLot + i;
            int serialEnd = serialStart + countPerLot - 1;

            // v1 코드에는 이 로직이 있었으나, 불필요해 보이므로 주석 처리하거나 단순화할 수 있습니다.
            // 여기서는 v1과 동일하게 유지합니다.
            if (i % resetInterval == 0) {
                serialEnd = serialStart;
            } else if ((i + 1) % resetInterval == 0) {
                serialEnd = serialStart + 1998;
            }

            lotMap.put(String.valueOf(lot), new SerialRange(serialStart, serialEnd));

            if ((i + 1) % resetInterval == 0) {
                serialStart = 1;
            } else {
                serialStart = serialEnd + 1;
            }
        }
        return lotMap;
    }

    /**
     * 알려진 모든 Lot 번호를 Set으로 반환합니다. (Fake/Tamper 구분에 사용)
     */
    public Set<String> getAllKnownLots() {
        Set<String> lots = new HashSet<>();
        factoryLotSerialRanges.values().forEach(lotMap -> lots.addAll(lotMap.keySet()));
        return lots;
    }

    /**
     * 시리얼 번호가 정의된 범위 내에 존재할 '가능성'이 있는지 빠르게 검사합니다.
     */
    public boolean isPotentiallyValidSerial(int serialNumber) {
        if (serialNumber <= 0) return false;
        for (Map<String, SerialRange> lotMap : factoryLotSerialRanges.values()) {
            for (SerialRange range : lotMap.values()) {
                if (range.contains(serialNumber)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 특정 공장과 Lot에 대해 시리얼 번호가 유효한지 정확히 검사합니다.
     */
    public boolean isValid(String factory, String lot, int serialNumber) {
        if (factory == null || lot == null) return false;
        Map<String, SerialRange> lotRanges = factoryLotSerialRanges.get(factory);
        if (lotRanges == null) {
            log.warn("시리얼 검증 실패: 정의되지 않은 factory입니다. factory={}", factory);
            return false;
        }
        SerialRange range = lotRanges.get(lot);
        return range != null && range.contains(serialNumber);
    }

    /**
     * Hub Type 또는 Scan Location 이름에서 생산 공장 이름을 추출합니다.
     */
    public String extractFactoryFromName(String hubType) {
        if (hubType == null) return null;
        if (hubType.contains("HWS") || hubType.contains("화성")) return "화성";
        if (hubType.contains("ICN") || hubType.contains("인천")) return "인천";
        if (hubType.contains("GUM") || hubType.contains("구미")) return "구미";
        // v1 코드에 '양산'이 'YAS'로 되어있어 수정. YGS가 맞다면 수정 필요
        if (hubType.contains("YGS") || hubType.contains("양산")) return "양산";
        return null;
    }
}