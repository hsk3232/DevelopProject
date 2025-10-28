package edu.pnu.service.analysis.be.api;

import edu.pnu.domain.BeAnalysis;
import edu.pnu.domain.EventHistory;
import edu.pnu.service.analysis.be.support.AssetCache;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 백엔드 규칙 기반 이상 탐지를 위한 표준 인터페이스입니다.
 * 각 구현체는 특정 유형의 이상 징후를 탐지하는 책임을 가집니다.
 */
public interface BeDetector {

    /**
     * 이상 징후를 탐지하여 BeAnalysis 엔티티 목록을 반환합니다.
     * @param eventsByEpc EPC 코드로 그룹화된 전체 이벤트 기록 맵
     * @param alreadyDetectedEpcIds 다른 탐지기에 의해 이미 이상으로 판정된 EPC ID Set
     * @param assetCache 미리 로드된 기준 정보 캐시
     * @return 탐지된 이상 징후(BeAnalysis) 목록
     */
    List<BeAnalysis> detect(Map<String, List<EventHistory>> eventsByEpc,
                            Set<String> alreadyDetectedEpcIds,
                            AssetCache assetCache);

    /**
     * 탐지기의 우선순위를 반환합니다. 숫자가 낮을수록 먼저 실행됩니다.
     * (예: Clone=0, RouteTampering=1, ProductAnomalies=2)
     * @return 우선순위 정수
     */
    int getPriority();
}