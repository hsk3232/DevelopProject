package edu.pnu.service.analysis.trips;

import edu.pnu.domain.AnalysisTrip;
import edu.pnu.domain.EventHistory;
import edu.pnu.repository.AnalysisTripRepository;
import edu.pnu.repository.EventHistoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisTripGenerationService {
    /*
     * [이동 경로 DB] 생성
     * [AnalysisTrips] AI 및 BE 경로 이상 탐지에 활용할 DB
     * 업로드한 Csv파일을 파싱/저장이 완료되면,
     * Batch Job을 활용해 저장된 Csv DB 속 이동경로를
     * From, To 형식으로 DB화함.
     * */
    private static final int BATCH_SIZE = 1000; // 배치 크기
    private final AnalysisTripRepository analysisTripRepo;
    private final EventHistoryRepository eventHistoryRepo;
    private final EntityManager entityManager;

    @Transactional
    public void generateTripsForFile(Long fileId) {
        log.info("[시작] : [AnalysisTripService] fileId={}의 AnalysisTrip 생성 시작", fileId);

        final int[] totalSavedCount = {0};
        // 이전 이벤트를 추적하기 위한 상태 변수. EPC가 바뀔 때마다 초기화됨.
        final EventHistory[] previousEventContainer = new EventHistory[1];
        List<AnalysisTrip> tripBatch = new ArrayList<>(BATCH_SIZE);

        log.debug("EventHistory 스트림 조회를 시작");
        // try-with-resources 구문으로 안전하게 스트림을 사용
        try (Stream<EventHistory> eventStream = eventHistoryRepo.streamWithDetailsByFileId(fileId)) {

            eventStream.forEach(currentEvent -> {
                EventHistory previousEvent = previousEventContainer[0];

                // 이전 이벤트가 존재하고, 두 이벤트가 동일한 EPC에 속하는지 확인
                // EPC가 달라졌다면, 새로운 EPC 그룹의 시작이므로 Trip을 생성하지 않음.
                if (previousEvent != null && previousEvent.getEpc().getEpcId().equals(currentEvent.getEpc().getEpcId())) {

                    // 버전 2의 새로운 AnalysisTrip 엔티티를 생성합니다.
                    AnalysisTrip trip = AnalysisTrip.builder()
                            .epc(currentEvent.getEpc()) // EPC 엔티티 참조
                            .fromLocationId(previousEvent.getCsvLocation().getLocationId())
                            .toLocationId(currentEvent.getCsvLocation().getLocationId())
                            .fromScanLocation(previousEvent.getCsvLocation().getScanLocation())
                            .toScanLocation(currentEvent.getCsvLocation().getScanLocation())
                            .fromBusinessStep(previousEvent.getBusinessStep())
                            .toBusinessStep(currentEvent.getBusinessStep())
                            .fromEventTime(previousEvent.getEventTime())
                            .toEventTime(currentEvent.getEventTime())
                            .relatedEventId(currentEvent.getEventId()) // 도착점 이벤트 ID 기록
                            .build();

                    tripBatch.add(trip);
                }

                // 배치 크기에 도달하면 DB에 저장하고 리스트를 비움.
                if (tripBatch.size() >= BATCH_SIZE) {
                    saveBatchAndClear(tripBatch, totalSavedCount);
                }

                // 현재 이벤트를 다음 순회를 위해 '이전 이벤트'로 업데이트
                previousEventContainer[0] = currentEvent;
            });

            // 마지막에 남은 배치 처리
            if (!tripBatch.isEmpty()) {
                saveBatchAndClear(tripBatch, totalSavedCount);
            }
        }
        log.info("[완료] : [AnalysisTripService] fileId={}의 AnalysisTrip 생성 완료. (총 저장 건수: {}건)", fileId, totalSavedCount[0]);
    }

    // JPA를 이용한 배치 저장 및 영속성 컨텍스트 관리 헬퍼 메서드
    private void saveBatchAndClear(List<AnalysisTrip> trips, int[] total) {
        analysisTripRepo.saveAll(trips);
        analysisTripRepo.flush();
        entityManager.clear();
        total[0] += trips.size();
        log.debug("[진행] : [AnalysisTripService] {}건 저장 (누적: {}건)", trips.size(), total[0]);
        trips.clear();
    }
}
