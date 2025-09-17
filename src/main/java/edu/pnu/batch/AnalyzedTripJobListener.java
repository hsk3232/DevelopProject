package edu.pnu.batch;

import edu.pnu.repository.CsvRepository;
import edu.pnu.service.analysis.AnalysisPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzedTripJobListener implements JobExecutionListener {
    private final AnalysisPipelineService analysisPipelineService;
    private final CsvRepository csvRepository;

    @Override
    public void afterJob(JobExecution jobExecution) {
        // 이 리스너가 다른 잡에도 붙을 수 있으니 이름 확인(안전)
        if (!"analyzedTripBatchJob".equals(jobExecution.getJobInstance().getJobName())) return;

        if (jobExecution.getStatus() != BatchStatus.COMPLETED) return;

        Long fileId = jobExecution.getJobParameters().getLong("fileId");
        if (fileId == null) return;

        // 업로더 userId 조회(웹소켓 채널 지정용)
        String userId = csvRepository.findById(fileId)
                .map(c -> c.getMember().getUserId())
                .orElse(null);

        log.info("[배치완료] analyzedTripBatchJob 완료 → 파이프라인 실행 (fileId={}, userId={})", fileId, userId);
        analysisPipelineService.runAnalysisPipeline(fileId, userId);
    }
}
