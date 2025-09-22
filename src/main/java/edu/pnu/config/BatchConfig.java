package edu.pnu.config;

import edu.pnu.batch.AfterTripJobPipelineTrigger;
import edu.pnu.service.analysis.trips.AnalysisTripGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@EnableBatchProcessing // Spring Batch 기능 활성화
@EnableScheduling
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepo; // batch에서 필수!
    private final PlatformTransactionManager transactionManager; // Step 생성에 필요
    private final AfterTripJobPipelineTrigger afterTripJobPipelineTrigger;
    private final AnalysisTripGenerationService analysisTripGenerationService;


    @Bean
// 정적분석 플러그인에서 	@Bean으로 선언한 메서드는 public이 아니어도 된다
    Job tripBuildJob(Step generateTripsStep) {
        return new JobBuilder("tripBuildJob", jobRepo)
                .incrementer(new RunIdIncrementer()) // RunIdIncrementer 추가
                .listener(afterTripJobPipelineTrigger)   // ★ 배치 완료 → 파이프라인 호출
                .start(generateTripsStep)             // ★ 주입된 파라미터 사용(괄호 X)
                .build();
    }


    @Bean
    Step generateTripsStep() {
        return new StepBuilder("generateTripsStep", jobRepo)
                .tasklet((contribution, chunkContext) -> {

                    // 1. fileId를 JobParameter에서 꺼내옴.
                    Long fileId = Long.valueOf(chunkContext.getStepContext()
                            .getJobParameters()
                            .get("fileId").toString());

                    log.info("[실행] : [BatchConfig] Step 실행됨!");
                    // [1] 새로운 Csv 저장되면, AnalyzedTsrip 로직 저장
                    analysisTripGenerationService.generateTripsForFile(fileId);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
