package edu.pnu.controller;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.dto.CsvFileDTO;
import edu.pnu.service.AnalysisPipelineService;
import edu.pnu.service.CsvLogService;
import edu.pnu.service.CsvSaveService;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@MultipartConfig(maxFileSize = 1024 * 1024 * 200, // 100mb
        maxRequestSize = 1024 * 1024 * 500 // 500mb
)
@RequestMapping("/api/manager")
public class CsvController {

    private final CsvSaveService csvSaveService;
    private final CsvLogService csvLogService;
    private final AnalysisPipelineService analysisPipelineService;


    // Front -> Back csv 전달 및 저장
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) {

        log.info("[API-REQ] CSV 파일 업로드 요청 - user: {}, file: {}", user.getUsername(), file.getOriginalFilename());

        Long fileId = csvSaveService.postCsvAndTriggerAsyncProcessing(file, user);

        // 기존 응답 JSON 구조와 동일하게 Map을 생성하여 반환
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "업로드 시작됨. 파일 ID: " + fileId + ". 진행상황은 실시간으로 알림됩니다.");

        return ResponseEntity.ok(responseBody);
    }


    // 업로드된 file 목록 조회
    @GetMapping("/upload/filelist")
    public ResponseEntity<Map<String, Object>> getFileList(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal CustomUserDetails user) {

        CsvFileDTO.FileListResponse fileListResponse = csvLogService.getFileListByCursor(
                cursor, size, search, user.getLocationId());

        // 내부적으로는 DTO를 사용하지만, 최종 응답은 기존과 동일한 Map 형태로 변환
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("data", fileListResponse.getData());
        responseBody.put("nextCursor", fileListResponse.getNextCursor());

        return ResponseEntity.ok(responseBody);
    }


    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadCsv(
            @PathVariable Long fileId,
            HttpServletRequest request) {

        Resource resource = csvLogService.loadCsvResource(fileId);
        String originalFilename = csvLogService.getOriginalFilename(fileId);

        String encodedFilename = encodeFilename(originalFilename, request.getHeader("User-Agent"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(resource);
    }

    @PostMapping("/resend/{fileId}")
    public ResponseEntity<String> resendAnalysis(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserDetails user) {

        log.info("[API-REQ] 분석 재요청 - user: {}, fileId: {}", user.getUsername(), fileId);
        analysisPipelineService.runAnalysisPipeline(fileId, user.getUserId());

        try {
            // 기존 응답과 동일하게 String 반환
            return ResponseEntity.ok("AI 모듈로 재전송 성공!");
        } catch (Exception e) {
            // 실제로는 GlobalExceptionHandler에서 처리하는 것이 더 좋음
            log.error("[오류] : [CsvController] 재전송 오류", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 모듈로 재전송 중 알 수 없는 오류가 발생했습니다."
            );
        }
    }

    private String encodeFilename(String filename, String userAgent) {
        try {
            if (userAgent != null && (userAgent.contains("MSIE") || userAgent.contains("Trident") || userAgent.contains("Edge"))) {
                return URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            } else {
                return new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            }
        } catch (Exception e) {
            log.warn("파일명 인코딩 실패: {}", filename, e);
            return "data.csv";
        }
    }

}