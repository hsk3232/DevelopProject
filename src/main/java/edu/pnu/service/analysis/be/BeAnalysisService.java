package edu.pnu.service.analysis.be;

import edu.pnu.domain.BeAnalysis;
import edu.pnu.domain.EventHistory;
import edu.pnu.repository.EventHistoryRepository;
import edu.pnu.service.analysis.be.api.BeDetector;
import edu.pnu.service.analysis.be.support.AssetCache;
import edu.pnu.service.analysis.be.support.BeAnalysisBatchSaver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeAnalysisService {
    private final EventHistoryRepository eventHistoryRepo;
    private final BeAnalysisBatchSaver batchSaver;
    private final AssetCache assetCache;
    private final List<BeDetector> detectors;

    @Transactional
    public void executeAnalysis(Long fileId) {
        log.info("[시작] fileId={}의 백엔드 이상 탐지(BeAnalysis)를 시작합니다.", fileId);

        // 1. 분석에 필요한 모든 데이터를 한 번에 로드 (N+1 문제 방지)
        List<EventHistory> allEvents = eventHistoryRepo.findAllWithDetailsByFileId(fileId);
        if (allEvents.isEmpty()) {
            log.warn("fileId={}에 대한 이벤트가 없어 분석을 종료합니다.", fileId);
            return;
        }

        // 2. EPC 코드로 데이터를 그룹화
        Map<String, List<EventHistory>> eventsByEpc = allEvents.stream()
                .collect(Collectors.groupingBy(event -> event.getEpc().getEpcCode()));

        List<BeAnalysis> totalAnomalies = new ArrayList<>();
        Set<String> alreadyDetectedEpcIds = new HashSet<>();

        // 3. 우선순위에 따라 Detector들을 정렬하여 순차적으로 실행
        detectors.stream()
                .sorted(Comparator.comparingInt(BeDetector::getPriority))
                .forEach(detector -> {
                    log.info("[진행] [{}] 탐지를 시작합니다.", detector.getClass().getSimpleName());
                    List<BeAnalysis> foundAnomalies = detector.detect(eventsByEpc, alreadyDetectedEpcIds, assetCache);
                    if (!foundAnomalies.isEmpty()) {
                        totalAnomalies.addAll(foundAnomalies);
                        log.info("[완료] [{}] 에서 {}건의 이상을 탐지했습니다.", detector.getClass().getSimpleName(), foundAnomalies.size());
                    }
                });

        // 4. 탐지된 모든 이상 징후를 배치 저장
        if (!totalAnomalies.isEmpty()) {
            batchSaver.saveAll(totalAnomalies);
        }

        log.info("[완료] fileId={}의 백엔드 이상 탐지를 완료했습니다. 총 {}건의 이상 발견.", fileId, totalAnomalies.size());
    }
}
