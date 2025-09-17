package edu.pnu.batch;

import edu.pnu.events.DbSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component // 스프링 빈(Bean)으로 등록, 자동 의존성 주입 가능
@RequiredArgsConstructor
public class BatchTriggerListener {


    private final JobLauncher jobLauncher; // Spring Batch의 Job 실행을 담당하는 런처(필수)
    private final Job analyzedTripBatchJob; // 실행할 Batch Job / BatchConfig에서 생성된 Job bean

    // [이벤트 리스너] - EventHistorySavedEvent가 발생하면 자동 호출됨
    // 주로 CSV 업로드 완료 후, Service에서 eventPublisher.publishEvent()로 발행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(DbSavedEvent event) throws Exception {

        log.info("[배치트리거] : [BatchTriggerListener] CSV 업로드 로그 ID = " + event.getFileId() + "로 배치 진입");

        // [JobParameters] - Spring Batch에서는 파라미터가 동일하면 이미 실행된 Job으로 간주하고 다시 실행하지 않음
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())  // - 매번 고유한 값(여기서는 현재시간 time)을 추가해서 항상 새로운 JobInstance로 실행되게 함
                .addLong("fileId", event.getFileId())
                .toJobParameters();

        // [Job 실행]
        // - analyzedTripBatchJob을 jobLauncher를 통해 실행
        // - 실제 배치 Step 및 로직은 MyBatchConfig 등에서 별도로 정의
        jobLauncher.run(analyzedTripBatchJob, params);


    }
}