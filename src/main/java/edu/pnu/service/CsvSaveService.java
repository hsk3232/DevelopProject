package edu.pnu.service;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.repository.CsvLocationRepository;
import edu.pnu.repository.CsvProductRepository;
import edu.pnu.repository.CsvRepository;
import edu.pnu.repository.EpcRepository;
import edu.pnu.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveService {
	
	private final CsvProductRepository csvProductRepo;
	private final CsvLocationRepository csvLocationRepo;
	private final CsvRepository csvRepo;
	private final EpcRepository epcRepo;
	private final MemberRepository memberRepo;
	
	private final CsvSaveBatchService csvSaveBatchService;
	private final StatisticsAdminService statistcsAdminService;
	private final WebSocketService webSocketService;
	
	@Qualifier("taskExecutor") // 비동기 작업을 위한 별도 스레드 풀 주입
    private final Executor taskExecutor;
	
	
	@Value("${file.upload.dir}")
    private String fileUploadDir; // 업로드 파일 재다운로드용
	private final int chunkSize = 1000; // 한 번에 읽어 처리할 row 수 (청크 단위)
	
	
	@Transactional
	public Long postCsvAndTriggerAsyncProcessing(MultipartFile file, CustomUserDetails user) {
		log.debug("[시작] : [CsvSaveService] CSV 파일 업로드 요청 처리 시작 - file: {}", file.getOriginalFilename());
		
		final String userId = user.getUserId();
		
	}
	
	
	
	
	
	
	

}
