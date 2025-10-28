package edu.pnu.service.analysis.be.support;

import edu.pnu.domain.BeAnalysis;
import edu.pnu.repository.BeAnalysisRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeAnalysisBatchSaver {
    private static final int BATCH_SIZE = 1000; // 한 번에 저장할 엔티티 개수

    private final BeAnalysisRepository beAnalysisRepo;
    private final EntityManager entityManager; // 영속성 컨텍스트를 직접 제어하기 위해 주입

    /**
     * BeAnalysis 엔티티 리스트를 받아 배치 단위로 저장합니다.
     * @param anomalies 저장할 BeAnalysis 엔티티 목록
     */
    // 이 메서드는 부모 트랜잭션과 독립적으로 실행되어야 메모리 관리에 유리하므로, Propagation.REQUIRES_NEW를 사용합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAll(List<BeAnalysis> anomalies) {
        if (anomalies == null || anomalies.isEmpty()) {
            return; // 저장할 데이터가 없으면 즉시 종료
        }

        int totalSavedCount = 0;

        // BATCH_SIZE 단위로 리스트를 나누어 처리
        for (int i = 0; i < anomalies.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, anomalies.size());
            List<BeAnalysis> batch = anomalies.subList(i, end);

            // 1. 배치 저장
            beAnalysisRepo.saveAll(batch);

            // 2. DB에 즉시 SQL 전송 (선택적이지만 메모리 관리와 관련 있음)
            beAnalysisRepo.flush();

            // 3. 영속성 컨텍스트에서 관리되던 엔티티들을 분리 (가장 중요!)
            //    이 작업을 하지 않으면, 저장된 모든 엔티티가 메모리에 계속 남아 메모리 부족(OOM)을 유발할 수 있습니다.
            entityManager.clear();

            totalSavedCount += batch.size();
            log.info("[BE Analysis Batch Save] {} 건 저장 완료 (누적: {} / {})", batch.size(), totalSavedCount, anomalies.size());
        }
    }
}
