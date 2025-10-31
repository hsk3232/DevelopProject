package edu.pnu.service.analysis.be;

import edu.pnu.domain.CsvRoute;
import edu.pnu.domain.BeAnalysis;
import edu.pnu.domain.EventHistory;
import edu.pnu.repository.CsvRouteRepository;
import edu.pnu.repository.EventHistoryRepository;
import edu.pnu.service.analysis.be.api.BeDetector;
import edu.pnu.service.analysis.be.support.AssetCache;
import edu.pnu.service.analysis.be.support.BeAnalysisBatchSaver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeAnalysisService {
    private final EventHistoryRepository eventHistoryRepo;
    private final CsvRouteRepository analysisTripRepo;
    private final BeAnalysisBatchSaver batchSaver;
    private final AssetCache assetCache;
    private final List<BeDetector> detectors;

    @Transactional(readOnly = true)
    public void executeAnalysis(Long fileId) {
        log.info("[시작] fileId={}의 백엔드 이상 탐지(BeAnalysis)를 시작합니다.", fileId);

        // 1. 분석에 필요한 모든 EventHistory와 AnalysisTrip 데이터를 한 번에 로드
        List<EventHistory> allEvents = eventHistoryRepo.findAllWithDetailsByFileId(fileId);
        if (allEvents.isEmpty()) {
            log.warn("fileId={}에 대한 이벤트가 없어 분석을 종료합니다.", fileId);
            return;
        }

        // [수정됨] AnalysisTrip 데이터도 미리 로드하여 EPC별로 그룹화합니다.
        Map<String, List<CsvRoute>> tripsByEpc = new HashMap<>();
        try (Stream<CsvRoute> s = analysisTripRepo.streamByEpc_CsvFile_FileId(fileId)) {
            s.forEach(t -> tripsByEpc.computeIfAbsent(t.getEpc().getEpcCode(), k -> new ArrayList<>()).add(t));
        }

        // 2. EPC 코드로 EventHistory 그룹화 및 시간순 정렬
        Map<String, List<EventHistory>> eventsByEpc = allEvents.stream()
                .collect(Collectors.groupingBy(event -> event.getEpc().getEpcCode()));

        eventsByEpc.values().forEach(list -> list.sort(Comparator.comparing(EventHistory::getEventTime)));

        List<BeAnalysis> totalAnomalies = new ArrayList<>();
        Set<String> alreadyDetectedEpcIds = new HashSet<>();

        // 3. 우선순위에 따라 Detector 순차 실행
        detectors.stream()
                .sorted(Comparator.comparingInt(BeDetector::getPriority))
                .forEach(detector -> {
                    String detectorName = detector.getClass().getSimpleName();
                    log.info("[진행] [{}] 탐지를 시작합니다.", detectorName);

                    // [수정됨] tripsByEpc 맵도 파라미터로 전달합니다.
                    List<BeAnalysis> found = detector.detect(eventsByEpc, tripsByEpc, alreadyDetectedEpcIds, assetCache);

                    if (!found.isEmpty()) {
                        totalAnomalies.addAll(found);
                        log.info("[완료] [{}] 에서 {}건의 이상을 탐지했습니다.", detectorName, found.size());
                    }
                });

        // 4. 최종 결과 배치 저장
        if (!totalAnomalies.isEmpty()) {
            batchSaver.saveAll(totalAnomalies);
        }

        log.info("[완료] fileId={}의 백엔드 이상 탐지를 완료했습니다. 총 {}건의 이상 발견.", fileId, totalAnomalies.size());
    }
}
