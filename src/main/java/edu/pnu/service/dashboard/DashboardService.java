package edu.pnu.service.dashboard;

import edu.pnu.domain.AnalysisSummary;
import edu.pnu.dto.DashboardDTO;
import edu.pnu.repository.AnalysisSummaryRepository;
import edu.pnu.repository.BeAnalysisRepository;
import edu.pnu.repository.EventHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final AnalysisSummaryRepository analysisSummaryRepo;
    private final EventHistoryRepository eventHistoryRepo;
    private final BeAnalysisRepository beAnalysisRepo;

    public DashboardDTO.KpiResponse getKPIAnalysis(Long fileId) {
        log.info("[진입] : [StatisticsFindService] KPI 전송 서비스");
        AnalysisSummary k = analysisSummaryRepo.findByCsv_FileId(fileId);
        DashboardDTO.KpiResponse dto = DashboardDTO.KpiResponse.fromEntity(k);
        return dto;
    }

    public List<DashboardDTO.InventoryItem> getInventory (Long fileId){
        log.info("[진입] : [StatisticsFindService] Inventory 전송 서비스");
        return eventHistoryRepo.calculateInventoryByBusinessStep(fileId);
    }

    public List<DashboardDTO.ByProductInfo> getByProduct (Long fileId){
        log.info("[진입] : [StatisticsFindService] ByProduct 전송 서비스");
        return beAnalysisRepo.countByProduct(fileId);
    }

}
