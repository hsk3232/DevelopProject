package edu.pnu.config;

import org.springframework.batch.core.Job;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import edu.pnu.service.BatchTriggerService;
import edu.pnu.service.SecurityUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BatchConfig {
	
	private final JobRepository jobRepo; // ⭐️ batch에서 필수!
    private final PlatformTransactionManager transactionManager; // ⭐️ Step 생성에 필요
	
    private final BatchTriggerService batchTriggerService;
    
    
	@Bean// 정적분석 플러그인에서 	@Bean으로 선언한 메서드는 public이 아니어도 된다
	Job analyzedTripBatchJob(Step analyzedTripStep) {
		return new JobBuilder("analyzedTripBatchJob", jobRepo)
				.incrementer(new RunIdIncrementer()) // RunIdIncrementer 추가
	            .start(analyzedTripStep()) // StepScope를 위해 null 전달
				.build();
	}

	
	@Bean
	Step analyzedTripStep() {
	    return new StepBuilder("analyzedTripStep", jobRepo)
	        .tasklet((contribution, chunkContext) -> {
	            // 여기에 배치 작업 로직 작성 (예: 로그, DB 저장 등)
	        	
	        	// 1️⃣ fileId를 JobParameter에서 꺼내온다
	            Long fileId = Long.valueOf(chunkContext.getStepContext()
                        .getJobParameters()
                        .get("fileId").toString());
	            
	        	log.info("[실행] : [MyBatchConfig] Step 실행됨!");
	        	// [1] 새로운 Csv 저장되면, AnalyzedTsrip 로직 저장
	        	batchTriggerService.analyzeAndSaveAllTripsBatch(fileId); 
	        	
	            return RepeatStatus.FINISHED;
	        }, transactionManager)
	        .build();
	}
}
