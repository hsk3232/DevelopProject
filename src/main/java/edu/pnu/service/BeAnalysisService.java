package edu.pnu.service;

import edu.pnu.domain.AnalysisTrip;
import edu.pnu.domain.BeAnalysis;
import edu.pnu.repository.AnalysisTripRepository;
import edu.pnu.repository.AssetRouteRepository;
import edu.pnu.repository.BeAnalysisRepository;
import edu.pnu.repository.EventHistoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeAnalysisService {

    private static final int BATCH_SIZE = 1000;
    private final AssetRouteRepository assetRouteRepo;
    private final AnalysisTripRepository analysisTripRepo;
    private final BeAnalysisRepository beAnalysisRepo;
    private final EventHistoryRepository eventHistoryRepo;
    private final EntityManager entityManager;

    @Transactional
    public void executeAnalysis(Long fileId) {
        log.info("[시작] fileId={}의 백엔드 이상 탐지(BeAnalysis)를 시작합니다.", fileId);

        // 다양한 이상 탐지 메서드를 순차적으로 호출
        detectRouteTampering(fileId);
        // detectClones(fileId); // TODO: 클론 탐지 로직 구현
        // detectFakes(fileId);   // TODO: 가품 탐지 로직 구현

        log.info("[완료] fileId={}의 백엔드 이상 탐지를 완료했습니다.", fileId);
    }

    /**
     * [Tamper] 정해진 경로를 이탈하는 경우를 탐지
     *
     * @param fileId 분석할 파일 ID
     */
    private void detectRouteTampering(Long fileId) {
        log.info("[진행] 경로 위반(Tamper) 탐지를 시작합니다.");

        // 1. AssetRoute에서 모든 정상 경로 정보를 조회하여 Set으로 변환 (빠른 조회를 위함)
        Set<String> validRoutes = assetRouteRepo.findAll().stream()
                .map(route -> route.getFromLocationId().getLocationId() + "->" + route.getToLocationId().getLocationId())
                .collect(Collectors.toSet());

        if (validRoutes.isEmpty()) {
            log.warn("정의된 정상 경로(AssetRoute)가 없어 경로 위반 탐지를 건너뜁니다.");
            return;
        }

        List<BeAnalysis> anomalies = new ArrayList<>();

        // 2. 해당 파일의 모든 이동 경로(AnalysisTrip)를 스트림으로 조회
        try (Stream<AnalysisTrip> tripStream = analysisTripRepo.streamByEpc_Csv_FileId(fileId)) {
            tripStream.forEach(trip -> {
                String currentRoute = trip.getFromLocationId() + "->" + trip.getToLocationId();

                // 3. 현재 이동 경로가 정상 경로 Set에 포함되지 않는 경우, '경로 위반'으로 판단
                if (!validRoutes.contains(currentRoute)) {
                    // EventHistory를 조회하여 BeAnalysis와 연결합니다.
                    eventHistoryRepo.findById(trip.getRelatedEventId()).ifPresent(event -> {
                        BeAnalysis anomaly = BeAnalysis.builder()
                                .eventHistory(event)
                                .anomalyType("Tamper")
                                .anomalyDetailedType("Route Violation")
                                .build();
                        anomalies.add(anomaly);
                    });
                }

                // 4. 배치 사이즈에 도달하면 저장
                if (anomalies.size() >= BATCH_SIZE) {
                    saveBatchAndClear(anomalies);
                }
            });
        }

        // 마지막 남은 배치 저장
        if (!anomalies.isEmpty()) {
            saveBatchAndClear(anomalies);
        }
        log.info("[완료] 경로 위반(Tamper) 탐지 완료.");
    }

    // TODO: 클론(Clone) 탐지 로직 구현
    private void detectClones(Long fileId) {
        // 로직 예시: 같은 EPC가 비슷한 시간에 다른 장소에서 나타나는 경우
    }

    // TODO: 가품(Fake) 탐지 로직 구현
    private void detectFakes(Long fileId) {
        // 로직 예시: 'Factory' 이벤트 없이 중간 단계에서 갑자기 등장하는 경우
    }

    private void saveBatchAndClear(List<BeAnalysis> anomalies) {
        beAnalysisRepo.saveAll(anomalies);
        beAnalysisRepo.flush();
        entityManager.clear();
        log.info("[진행] 이상 징후 {}건 저장.", anomalies.size());
        anomalies.clear();
    }
}
