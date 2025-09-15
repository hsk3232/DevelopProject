package edu.pnu.service.csv;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Csv;
import edu.pnu.domain.CsvLocation;
import edu.pnu.domain.CsvProduct;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Member;
import edu.pnu.exception.BadRequestException;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.exception.CsvFileSaveToDiskException;
import edu.pnu.exception.FileUploadException;
import edu.pnu.exception.InvalidCsvFormatException;
import edu.pnu.repository.CsvLocationRepository;
import edu.pnu.repository.CsvProductRepository;
import edu.pnu.repository.CsvRepository;
import edu.pnu.repository.EpcRepository;
import edu.pnu.repository.MemberRepository;
import edu.pnu.service.analysis.AnalysisPipelineService;
import edu.pnu.service.messaging.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.opencsv.CSVReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveService {

    private static final int CHUNK_SIZE = 1000; // 필요시 설정값으로 변경 가능
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CsvProductRepository csvProductRepo;
    private final CsvLocationRepository csvLocationRepo;
    private final CsvRepository csvRepo;
    private final EpcRepository epcRepo;
    private final MemberRepository memberRepo;

    private final CsvSaveBatchService csvSaveBatchService;
    private final WebSocketService webSocketService;
    private final AnalysisPipelineService analysisPipelineService;


    @Value("${file.upload.dir}")
    private String fileUploadDir; // 업로드 파일 재다운로드용

    private static String productKey(String company, String product) {
        return company + "|" + product;
    }

    // ■■■■■■■■■■■■■■ [ 1단계: CSV 저장/파싱 (동기) ] ■■■■■■■■■■■■■
    @Transactional
    public Long postCsvAndTriggerAsyncProcessing(MultipartFile file, CustomUserDetails user) {
        final String userId = user.getUserId();
        webSocketService.sendMessage(userId, "[1단계/CSV] START - 파일 업로드 시작");

        final Member member = memberRepo.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("회원 정보를 찾을 수 없습니다."));

        // [file 확장자 검사] .csv 여부 확인
        validateFile(file);

        final Csv csv = createAndSaveCsvMetadata(file, member);
        webSocketService.sendMessage(userId, "[1단계/CSV] INFO  - 메타데이터 저장 완료 (fileId=" + csv.getFileId() + ")");

        // [CSV Meta 정보 저장]
        storeFileToDisk(file, csv);
        webSocketService.sendMessage(userId, "[1단계/CSV] INFO  - 디스크 저장 완료: " + csv.getSavedFileName());

        final ImportCache cache = new ImportCache(
                new HashSet<>(csvLocationRepo.findAllLocationIds()),
                csvProductRepo.findAllByFileIdAsMap(csv.getFileId()),
                epcRepo.findAllByFileIdAsMap(csv.getFileId()),
                epcRepo.findAllEpcCodesByFileId(csv.getFileId())
        );

        webSocketService.sendMessage(userId, "[1단계/CSV] INFO  - 파싱 준비 완료. 청크 처리 시작");
        // [errorRows] 오류 Row
        final Map<String, List<Integer>> errorRows = parseAndProcessFile(csv, cache, userId);

        if (!errorRows.isEmpty()) {
            int total = errorRows.values().stream().mapToInt(List::size).sum();
            webSocketService.sendMessage(userId, "[1단계/CSV] WARN  - 파싱 오류 라인 " + total + "건 (세부는 서버 로그)");
        }

        webSocketService.sendMessage(userId, "[1단계/CSV] DONE  - CSV 저장 및 파싱 완료. 2단계 분석 시작");
        // [분석 및 통계 파이프라인] 호출
        analysisPipelineService.runAnalysisPipeline(csv.getFileId(), userId);
        return csv.getFileId();
    }

    // ■■■■■■■■■■■■■■ [ 2~4단계: 동기 저장 ] ■■■■■■■■■■■■■

    private Map<String, List<Integer>> parseAndProcessFile(Csv csv, ImportCache cache, String userId) {
        final Map<String, List<Integer>> errorRows = new HashMap<>();
        final Path path = Paths.get(fileUploadDir, csv.getSavedFileName());

        long processed = 0L;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVReader csvReader = new com.opencsv.CSVReaderBuilder(reader)
                     .withCSVParser(new com.opencsv.CSVParserBuilder()
                             .withSeparator(',')
                             .withQuoteChar('"')
                             .withEscapeChar('\\')
                             .withIgnoreLeadingWhiteSpace(true)
                             .build())
                     .build()) {

            final String[] header = csvReader.readNext();
            final Map<String, Integer> colIdx = getColumnIndexMap(header);
            final Set<Long> seenEventKeys = new HashSet<>();

            String[] row;
            List<String[]> chunk = new ArrayList<>(CHUNK_SIZE);
            int rowNum = 1; // header

            while ((row = csvReader.readNext()) != null) {
                rowNum++;
                chunk.add(row);
                if (chunk.size() >= CHUNK_SIZE) {
                    int startRowNum = rowNum - CHUNK_SIZE;
                    processChunk(chunk, colIdx, csv, errorRows, startRowNum, cache, seenEventKeys);
                    processed += chunk.size();
                    webSocketService.sendMessage(userId, "[1단계/CSV] PROG  - 파싱 진행: " + processed + "행 처리");
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                int startRowNum = rowNum - chunk.size();
                processChunk(chunk, colIdx, csv, errorRows, startRowNum, cache, seenEventKeys);
                processed += chunk.size();
                webSocketService.sendMessage(userId, "[1단계/CSV] PROG  - 파싱 진행: " + processed + "행 처리 (마지막 청크)");
            }

        } catch (IOException e) {
            webSocketService.sendMessage(userId, "[1단계/CSV] ERROR - CSV 파일 IO 오류: " + e.getMessage());
            throw new RuntimeException("CSV 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            webSocketService.sendMessage(userId, "[1단계/CSV] ERROR - CSV 처리 중 치명적 오류: " + e.getMessage());
            throw new RuntimeException("CSV 처리 중 오류가 발생했습니다.", e);
        }
        return errorRows;
    }


    // ■■■■■■■■■■■■■■ [ 청크 처리 ] ■■■■■■■■■■■■■
    private void processChunk(List<String[]> chunk,
                              Map<String, Integer> colIdx,
                              Csv csv,
                              Map<String, List<Integer>> errorRows,
                              int startRowNum,
                              ImportCache cache,
                              Set<Long> seenEventKeys) {

        List<CsvLocation> newLocations = new ArrayList<>();
        List<CsvProduct> newProducts = new ArrayList<>();
        List<Epc> newEpcs = new ArrayList<>();
        List<EventHistory> newEvents = new ArrayList<>();

        // 청크 내부 중복 제거 (문자열 키 사용)
        Set<String> eventDedupInChunk = new HashSet<>();

        // 1) 마스터 파싱 (Location/Product/EPC)
        for (String[] row : chunk) {
            // Location (전역 유일)
            Long locationId = parseLongSafe(getValue(colIdx, row, "location_id"));
            if (locationId != null && cache.locationIds.add(locationId)) {
                newLocations.add(CsvLocation.builder()
                        .csv(csv)
                        .locationId(locationId)
                        .scanLocation(getValue(colIdx, row, "scan_location"))
                        .operatorId(parseLongSafe(getValue(colIdx, row, "operator_id")))
                        .deviceId(parseLongSafe(getValue(colIdx, row, "device_id")))
                        .build());
            }

            // Product (파일 단위 유니크)
            String epcProduct = getValue(colIdx, row, "epc_product");
            String epcCompany = getValue(colIdx, row, "epc_company");
            if (epcProduct != null && epcCompany != null) {
                String key = productKey(epcCompany, epcProduct);
                if (!cache.productMap.containsKey(key)) {
                    CsvProduct p = CsvProduct.builder()
                            .csv(csv)
                            .epcProduct(epcProduct)
                            .epcCompany(epcCompany)
                            .productName(getValue(colIdx, row, "product_name"))
                            .build();
                    newProducts.add(p);
                    cache.productMap.put(key, p); // 메모리 선반영 (ID는 save 후 채워짐)
                }
            }

            // EPC (파일 단위 유니크)
            String epcCode = getValue(colIdx, row, "epc_code");
            if (epcCode != null && cache.epcCodes.add(epcCode)) {
                newEpcs.add(Epc.builder()
                        .csv(csv)
                        .epcCode(epcCode)
                        .epcHeader(getValue(colIdx, row, "epc_header"))
                        .epcLot(getValue(colIdx, row, "epc_lot"))
                        .epcSerial(getValue(colIdx, row, "epc_serial"))
                        .manufactureDate(tryParseDateTime(getValue(colIdx, row, "manufacture_date"),
                                DATE_TIME_FORMATTER, null, 0, ""))
                        .expiryDate(tryParseDate(getValue(colIdx, row, "expiry_date"),
                                DATE_FORMATTER, null, 0, ""))
                        .build());
            }
        }

        // 2) 배치 저장 (REQUIRES_NEW 권장: CsvSaveBatchService 내부에서 처리)
        if (!newLocations.isEmpty()) csvSaveBatchService.saveCsvLocations(newLocations);
        if (!newProducts.isEmpty()) csvSaveBatchService.saveCsvProducts(newProducts);
        if (!newEpcs.isEmpty()) csvSaveBatchService.saveEpcs(newEpcs);

        // 저장 완료된 엔티티로 캐시 확정(신규만 반영)
        for (CsvProduct p : newProducts) {
            cache.productMap.put(productKey(p.getEpcCompany(), p.getEpcProduct()), p);
        }
        for (Epc e : newEpcs) {
            cache.epcMap.put(e.getEpcCode(), e);
        }

        // 3) EventHistory 파싱/중복 제거/저장
        for (int i = 0; i < chunk.size(); i++) {
            String[] row = chunk.get(i);
            int currentRowNum = startRowNum + i;

            String epcCode = getValue(colIdx, row, "epc_code");
            String epcCompany = getValue(colIdx, row, "epc_company");
            String epcProduct = getValue(colIdx, row, "epc_product");
            Long locationId = parseLongSafe(getValue(colIdx, row, "location_id"));

            Epc epc = epcCode != null ? cache.epcMap.get(epcCode) : null;
            CsvProduct product = (epcCompany != null && epcProduct != null)
                    ? cache.productMap.get(productKey(epcCompany, epcProduct))
                    : null;
            CsvLocation locationRef = (locationId != null) ? csvLocationRepo.getReferenceById(locationId) : null;

            LocalDateTime evTime = tryParseDateTime(getValue(colIdx, row, "event_time"),
                    DATE_TIME_FORMATTER, errorRows, currentRowNum, "event_time");
            String originalStep = getValue(colIdx, row, "business_step");
            String step = normalizeBusinessStep(originalStep);
            String type = getValue(colIdx, row, "event_type");
            String hub = getValue(colIdx, row, "hub_type");

            // DB 유니크 제약과 동일한 구성으로 키 생성
            String keyStr = buildEventKey(csv.getFileId(), epc, locationId, product, evTime, step, type);
            long keyHash = hash64(keyStr);

            if (epc != null && product != null && locationRef != null
                    && evTime != null // 권장: null이면 저장하지 않음(유니크 제약 일관성)
                    && eventDedupInChunk.add(keyStr)
                    && seenEventKeys.add(keyHash)) {

                newEvents.add(EventHistory.builder()
                        .csv(csv)
                        .epc(epc)
                        .csvProduct(product)
                        .csvLocation(locationRef)
                        .eventTime(evTime)
                        .businessStep(step)
                        .eventType(type)
                        .hubType(hub)
                        .businessOriginal(originalStep)
                        .build());
            } else {
                errorRows.computeIfAbsent("참조오류/중복/시간누락", k -> new ArrayList<>()).add(currentRowNum);
            }
        }

        if (!newEvents.isEmpty()) csvSaveBatchService.saveEventHistories(newEvents);
    }

    // ■■■■■■■■■■■■■■ [ Helper ] ■■■■■■■■■■■■■

    // [file 확장자 검사] .csv 여부 확인
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new CsvFileNotFoundException("업로드된 파일이 없습니다.");
        String name = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        if (!name.endsWith(".csv")) {
            throw new FileUploadException("CSV 파일 형식이 아닙니다.");
        }
    }

    // [CSV Meta 정보 저장]
    private Csv createAndSaveCsvMetadata(MultipartFile file, Member member) {
        String originalFileName = file.getOriginalFilename();
        String savedFileName = UUID.randomUUID() + ".csv";
        Csv csv = Csv.builder()
                .fileName(originalFileName)
                .savedFileName(savedFileName)
                .filePath(fileUploadDir)
                .fileSize(file.getSize())
                .member(member)
                .build();
        return csvRepo.save(csv);
    }

    // [파일 저장] Csv 파일 BE 서버에 저장
    private void storeFileToDisk(MultipartFile file, Csv csv) {
        try {
            Path targetLocation = Paths.get(fileUploadDir).resolve(csv.getSavedFileName());
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", csv.getSavedFileName(), e);
            throw new CsvFileSaveToDiskException("파일을 디스크에 저장하는 데 실패했습니다.");
        }
    }

    private Map<String, Integer> getColumnIndexMap(String[] header) {
        if (header == null) throw new InvalidCsvFormatException("CSV 파일에 헤더가 없습니다.");
        Map<String, Integer> colIdx = new HashMap<>(header.length * 2);
        for (int i = 0; i < header.length; i++) {
            colIdx.put(header[i].trim().toLowerCase(), i);
        }
        return colIdx;
    }

    private String getValue(Map<String, Integer> colIdx, String[] row, String colName) {
        Integer idx = colIdx.get(colName.toLowerCase());
        if (idx == null || idx >= row.length) return null;
        String v = row[idx];
        return (v == null || v.isBlank()) ? null : v.trim();
        // 필요시 "NULL"/"null" 같은 문자열도 무시하도록 확장 가능
    }

    private Long parseLongSafe(String s) {
        if (s == null) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime tryParseDateTime(String value,
                                           DateTimeFormatter formatter,
                                           Map<String, List<Integer>> errorRows,
                                           int rowNum,
                                           String fieldName) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            if (errorRows != null) {
                errorRows.computeIfAbsent(fieldName + " 날짜 형식 오류", k -> new ArrayList<>()).add(rowNum);
            }
            return null;
        }
    }

    private LocalDate tryParseDate(String value,
                                   DateTimeFormatter formatter,
                                   Map<String, List<Integer>> errorRows,
                                   int rowNum,
                                   String fieldName) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException e) {
            if (errorRows != null) {
                errorRows.computeIfAbsent(fieldName + " 날짜 형식 오류", k -> new ArrayList<>()).add(rowNum);
            }
            return null;
        }
    }

    private String normalizeBusinessStep(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        if (s.contains("factory")) return "Factory";
        if (s.contains("wms")) return "WMS";
        if (s.contains("logistics_hub") || s.contains("logi") || s.contains("hub")) return "LogiHub";
        if (s.startsWith("w_stock")) return "Wholesaler";
        if (s.startsWith("r_stock")) return "Reseller";
        if (s.contains("pos")) return "POS";
        return input;
    }

    private String buildEventKey(Long fileId,
                                 Epc epc,
                                 Long locationId,
                                 CsvProduct product,
                                 LocalDateTime eventTime,
                                 String businessStep,
                                 String eventType) {
        String epoch = (eventTime != null)
                ? String.valueOf(eventTime.atZone(ZoneOffset.UTC).toEpochSecond())
                : "null";
        return String.join("|",
                String.valueOf(fileId),
                epc != null ? String.valueOf(epc.getEpcId()) : "null",
                locationId != null ? String.valueOf(locationId) : "null",
                product != null ? String.valueOf(product.getProductId()) : "null",
                epoch,
                businessStep != null ? businessStep : "null",
                eventType != null ? eventType : "null"
        );
    }

    private long hash64(String s) {
        // FNV-1a 64-bit
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    // ■■■■■■■■■■■■■■ [ 인메모리 캐시 ] ■■■■■■■■■■■■■
    private static final class ImportCache {
        final Set<Long> locationIds;              // 전역 locationId (CsvLocation PK) 중복 체크
        final Map<String, CsvProduct> productMap; // 파일 내 product 키 -> CsvProduct
        final Map<String, Epc> epcMap;            // 파일 내 epcCode -> Epc
        final Set<String> epcCodes;               // 파일 내 epcCode 중복 체크

        ImportCache(Set<Long> locationIds,
                    Map<String, CsvProduct> productMap,
                    Map<String, Epc> epcMap,
                    Set<String> epcCodes) {
            this.locationIds = locationIds;
            this.productMap = productMap;
            this.epcMap = epcMap;
            this.epcCodes = epcCodes;
        }
    }
}
