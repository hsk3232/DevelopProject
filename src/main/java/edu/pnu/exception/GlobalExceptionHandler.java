package edu.pnu.exception;

import java.io.FileNotFoundException;
import java.util.Map;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	  // 로그 필드 선언 (클래스 맨 위)
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	
	// 2. 파일 자체가 아예 존재하지 않을 때(DB/요청/업로드 모두)
    @ExceptionHandler(CsvFileNotFoundException.class)
    public ResponseEntity<String> handleCsvFileNotFound(RuntimeException ex) {
        // 로그 남기기 (원하면)
        // log.error("예외 발생!", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("에러 발생: " + ex.getMessage());
    }
    // 3. 경로나 실제 파일이 없거나 읽기 불가할 때
    @ExceptionHandler(CsvFilePathNotFoundException.class)
    public ResponseEntity<String> handleCsvFilePathNotFound(FileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("파일을 찾을 수 없습니다: " + ex.getMessage());
    }
    
    @ExceptionHandler(CsvFileSaveToDiskException.class)
    public ResponseEntity<String> handleCsvFileSaveToDiskFound(CsvFileSaveToDiskException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("파일을 저장할 수 없습니다: " + ex.getMessage());
    }
    
    
    //  4. 파일 접근 권한 예외
    @ExceptionHandler(FileAccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(FileAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("파일 접근 권한 없음: " + ex.getMessage());
    }
    
    // 파일 업로드 예외
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<String> handleFileUpload(FileUploadException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("파일 업로드 실패: " + ex.getMessage());
    }
    
    // CSV 포맷 오류 예외
    @ExceptionHandler(InvalidCsvFormatException.class)
    public ResponseEntity<String> handleInvalidCsv(InvalidCsvFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("CSV 형식 오류: " + ex.getMessage());
    }
    
    // 데이터 공유 서비스 전용 예외 (서비스 내부 오류 등)
    @ExceptionHandler(DataShareServiceException.class)
    public ResponseEntity<?> handleServiceException(DataShareServiceException ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", ex.getMessage()));
    }

    // 데이터 조회 결과가 없을 때 발생시키는 예외
    @ExceptionHandler(NoDataFoundException.class)
    public ResponseEntity<?> handleNoDataException(NoDataFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }
    
    //NodeList를 만들지 못할 때
    @ExceptionHandler(NotFoundExceptions.class)
    public ResponseEntity<String> NotFoundExceptions(NotFoundExceptions ex) {
        log.error("NotFoundException", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> UnauthorizedException(NodeNotFoundException ex) {
    	return ResponseEntity
    			.status(HttpStatus.FORBIDDEN)
    			.body("Node List 생성 오류" + ex.getMessage());
    }

    
    
    // 400(Bad Request): 사용자가 잘못 보낸 입력
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleBadRequest(BadRequestException ex) {
        log.error("BadRequestException", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // 403(Forbidden): 권한 없음
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbidden(ForbiddenException ex) {
        log.error("ForbiddenException", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
    
    // 404(Not Found): 리소스 없음
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        log.error("NotFoundException", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 예외 누락 방지용 - 500 및 기타 예외 전체 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부 오류: " + ex.getMessage());
    }
    
	
}
