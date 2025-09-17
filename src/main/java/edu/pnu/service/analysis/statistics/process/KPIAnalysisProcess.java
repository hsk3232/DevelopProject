package edu.pnu.service.analysis.statistics.process;

import edu.pnu.domain.AnalysisSummary;
import edu.pnu.domain.AnalysisTrip;
import edu.pnu.domain.Csv;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.repository.AiAnalysisRepository;
import edu.pnu.repository.AnalysisSummaryRepository;
import edu.pnu.repository.AnalysisTripRepository;
import edu.pnu.repository.BeAnalysisRepository;
import edu.pnu.repository.CsvRepository;
import edu.pnu.service.analysis.statistics.api.StatisticsInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KPIAnalysisProcess implements StatisticsInterface {
    private final AnalysisSummaryRepository analysisSummaryRepo;
    private final AnalysisTripRepository analysisTripRepo;
    private final AiAnalysisRepository aiAnalysisRepo;
    private final BeAnalysisRepository beAnalysisRepo;
    private final CsvRepository csvRepo;

    @Override
    public String getProcessorName() {
        return "최종 통계 집계(AnalysisSummary)";
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    @Transactional
    public void process(Long fileId) {
        Csv csv = csvRepo.findById(fileId)
                .orElseThrow(() -> new CsvFileNotFoundException("Csv not found: " + fileId));

        long totalTripCount = analysisTripRepo.countByEpc_Csv_FileId(fileId);
        long beAnomalyCount = beAnalysisRepo.countByEventHistory_Csv_FileId(fileId);
        long aiAnomalyCount = aiAnalysisRepo.countByEventHistory_Csv_FileId(fileId);
        double avgLeadTime = computeAverageLeadTimeSeconds(fileId);

        AnalysisSummary summary = analysisSummaryRepo.findById(fileId)
                .orElse(AnalysisSummary.builder().fileId(fileId).csv(csv).build());

        summary.setTotalEventCount(totalTripCount);
        summary.setTotalErrorCount((int) beAnomalyCount);
        summary.setAiTotalAnomalyCount((int) aiAnomalyCount);
        summary.setAvgLeadTime(avgLeadTime);

        analysisSummaryRepo.save(summary);
    }

    private double computeAverageLeadTimeSeconds(Long fileId) {
        long n = 0, sum = 0;
        try (Stream<AnalysisTrip> s = analysisTripRepo.streamByEpc_Csv_FileId(fileId)) {
            for (AnalysisTrip t : (Iterable<AnalysisTrip>) s::iterator) {
                if (t.getFromEventTime() != null && t.getToEventTime() != null) {
                    sum += Duration.between(t.getFromEventTime(), t.getToEventTime()).getSeconds();
                    n++;
                }
            }
        }
        return n == 0 ? 0.0 : (double) sum / n;
    }
}
