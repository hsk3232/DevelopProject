package edu.pnu.service.csv;

import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.CsvFile;
import edu.pnu.domain.CsvLocation;
import edu.pnu.domain.CsvProduct;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Member;
import edu.pnu.events.DbSavedEvent;
import edu.pnu.exception.BadRequestException;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.exception.CsvFileSaveToDiskException;
import edu.pnu.exception.FileUploadException;
import edu.pnu.exception.InvalidCsvFormatException;
import edu.pnu.repository.CsvLocationRepository;
import edu.pnu.repository.CsvProductRepository;
import edu.pnu.repository.CsvRouteRepository;
import edu.pnu.repository.EpcRepository;
import edu.pnu.repository.MemberRepository;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final CsvRouteRepository csvRepo;
    private final EpcRepository epcRepo;
    private final MemberRepository memberRepo;

    private final CsvSaveJdbcService csvSaveJdbcService;
    private final WebSocketService webSocketService;
    // private final AnalysisPipelineService analysisPipelineService;
    private final ApplicationEventPublisher publisher;

    @Value("${file.upload.dir}")
    private String fileUploadDir; // 업로드 파일 재다운로드용

    private static String productKey(String company, String product) {
        return company + "|" + product;
    }

    // ■■■■■■■■■■■■■■ [ 1단계: CSV 저장/파싱 (동기) ] ■■■■■■■■■■■■■
    // 업로드된 CSV를 저장·파싱하고 메타데이터 저장 후 비동기 분석 트리거(이벤트 발행)까지 한 번에 수행
    @Transactional
    public Long postCsvAndTriggerAsyncProcessing(MultipartFile file, CustomUserDetails user) {
        final String userId = user.getUserId();
        log.debug("[1] [CsvSaveService] [진입] : CsvSaveService 진입 성공 / 업로더 = {}", userId);
        webSocketService.sendMessage(userId, "[1단계/CSV] START - 파일 업로드 시작");

        final Member member = memberRepo.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("회원 정보를 찾을 수 없습니다."));

        // [file 확장자 검사] .csv 여부 확인
        validateFile(file);

        final CsvFile csv = createAndSaveCsvMetadata(file, member);
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

        // [분석 및 통계 파이프라인] 호출
        // analysisPipelineService.runAnalysisPipeline(csv.getFileId(), userId);
        // BatchTriggerEvent 생성
        publisher.publishEvent(new DbSavedEvent(csv.getFileId()));
        webSocketService.sendMessage(userId, "[1단계/CSV] DONE  - CSV 저장 및 파싱 완료. 2단계 분석 시작");
        return csv.getFileId();
    }

    // ■■■■■■■■■■■■■■ [ 2~4단계: 동기 저장 ] ■■■■■■■■■■■■■
    // 저장된 CSV 파일을 청크 단위로 읽어 파싱하고 각 청크를 처리하기 위해 processChunk를 호출
    private Map<String, List<Integer>> parseAndProcessFile(CsvFile csv, ImportCache cache, String userId) {
        final Map<String, List<Integer>> errorRows = new HashMap<>();
        final Path path = Paths.get(fileUploadDir, csv.getSavedFileName());

        long processed = 0L;

        log.debug("[5] [CsvSaveService] [진입] : [parseAndProcessFile] 파싱 프로세스 진입 성공");

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
    // 하나의 청크를 파싱하여 Location/Product/EPC를 수집·중복제거하고, 엔티티를 배치로 저장한 뒤 EventHistory를 생성
    private void processChunk(List<String[]> chunk,
                              Map<String, Integer> colIdx,
                              CsvFile csv,
                              Map<String, List<Integer>> errorRows,
                              int startRowNum,
                              ImportCache cache,
                              Set<Long> seenEventKeys) {
        log.debug("[진입] : [processChunk] 청크 처리를 위한 private 메서드 진입");
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
                        .csvFile(csv)
                        .csvLocationId(locationId)
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
                            .csvFile(csv)
                            .csvEpcProduct(epcProduct)
                            .csvEpcCompany(epcCompany)
                            .csvProductName(getValue(colIdx, row, "product_name"))
                            .build();
                    newProducts.add(p);
                    cache.productMap.put(key, p); // 메모리 선반영 (ID는 save 후 채워짐)
                }
            }

            // EPC (파일 단위 유니크)
            String epcCode = getValue(colIdx, row, "epc_code");
            if (epcCode != null && cache.epcCodes.add(epcCode)) {
                newEpcs.add(Epc.builder()
                        .csvFile(csv)
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

        // 2) 배치 저장 (REQUIRES_NEW 권장: CsvSaveJdbcService 내부에서 처리)
        if (!newLocations.isEmpty()) csvSaveJdbcService.saveCsvLocations(newLocations);
        if (!newProducts.isEmpty()) csvSaveJdbcService.saveCsvProducts(newProducts);
        if (!newEpcs.isEmpty()) csvSaveJdbcService.saveEpcs(newEpcs);

        // 저장 완료된 엔티티로 캐시 확정(신규만 반영)
        // 저장 완료된 엔티티로 캐시 확정(신규만 '영속 객체'로 반영)
        if (!newProducts.isEmpty()) {
            Map<String, CsvProduct> freshProducts = csvProductRepo.findAllByFileIdAsMap(csv.getFileId());
            for (CsvProduct p : newProducts) {
                String key = productKey(p.getCsvEpcProduct(), p.getCsvEpcProduct());
                CsvProduct persisted = freshProducts.get(key);
                if (persisted != null) {
                    cache.productMap.put(key, persisted); // ← ID가 채워진 객체로 교체
                } else {
                    // 안전장치: 혹시 INSERT 무시/중복 등으로 못 찾았을 때 대비
                    cache.productMap.remove(key);
                }
            }
        }
        if (!newEpcs.isEmpty()) {
            Map<String, Epc> freshEpcs = epcRepo.findAllByFileIdAsMap(csv.getFileId());
            for (Epc e : newEpcs) {
                String code = e.getEpcCode();
                Epc persisted = freshEpcs.get(code);
                if (persisted != null) {
                    cache.epcMap.put(code, persisted); // ← ID가 채워진 객체로 교체
                } else {
                    cache.epcMap.remove(code);
                }
            }
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
                        .csvFile(csv)
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

        if (!newEvents.isEmpty()) csvSaveJdbcService.saveEventHistories(newEvents);
    }

    // ■■■■■■■■■■■■■■ [ Helper ] ■■■■■■■■■■■■■

    // [file 확장자 검사] .csv 여부 확인
    private void validateFile(MultipartFile file) {
        log.debug("[2] [CsvSaveService] [진입] : [validateFile] file 확장자 검사");
        if (file == null || file.isEmpty())
            throw new CsvFileNotFoundException("업로드된 파일이 없습니다.");
        String name = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        if (!name.endsWith(".csv")) {
            throw new FileUploadException("CSV 파일 형식이 아닙니다.");
        }



        log.debug("[2] [CsvSaveService] [진입] : [validateFile] file 확장자 검사 완료");
    }

    // [CSV Meta 정보 저장] : 업로드 파일의 메타데이터(Csv 엔티티)를 생성·DB에 저장하고 저장된 Csv를 반환
    private CsvFile createAndSaveCsvMetadata(MultipartFile file, Member member) {
        log.debug("[3] [CsvSaveService] [진입] : [createAndSaveCsvMetadata] CSV Meta 정보 저장 진입");
        String originalFileName = file.getOriginalFilename();
        String savedFileName = UUID.randomUUID() + ".csv";
        CsvFile csv = CsvFile.builder()
                .fileName(originalFileName)
                .savedFileName(savedFileName)
                .filePath(fileUploadDir)
                .fileSize(file.getSize())
                .member(member)
                .build();
        log.debug("[3] [CsvSaveService] [진입] : [createAndSaveCsvMetadata] CSV Meta 정보 저장 성공, originalFileName = {}", originalFileName);
        return csvRepo.save(csv);
    }

    // [파일 저장] : 업로드된 MultipartFile을 지정된 디스크 위치(fileUploadDir)에 저장
    private void storeFileToDisk(MultipartFile file, CsvFile csvFile) {
        try {
            log.debug("[4] [CsvSaveService] [진입] : [storeFileToDisk] BE 서버에 저장 진입");
            Path targetLocation = Paths.get(fileUploadDir).resolve(csvFile.getSavedFileName());
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.debug("[4] [CsvSaveService] [완료] : [storeFileToDisk] BE 서버에 저장 완료");
        } catch (IOException e) {
            log.error("[4] [CsvSaveService] [진입] : [storeFileToDisk] 파일 저장 실패: {}", csvFile.getSavedFileName(), e);
            throw new CsvFileSaveToDiskException("파일을 디스크에 저장하는 데 실패했습니다.");
        }
    }

    // [필수 칼럼 확인] : CSV 헤더 배열로부터 컬럼명→인덱스 맵을 만들고 필수 컬럼 누락 체크를 수행
    private Map<String, Integer> getColumnIndexMap(String[] header) {
        if (header == null)
            throw new InvalidCsvFormatException("CSV 파일에 헤더가 없습니다.");
        Map<String, Integer> colIdx = new HashMap<>(header.length * 2);
        for (int i = 0; i < header.length; i++) {
            String h = header[i];
            if (h == null) continue;                    // ← null 방어
            String key = h.trim().toLowerCase();
            if (key.isEmpty()) continue;                // ← 빈 헤더 스킵
            colIdx.put(key, i);
        }
        List<String> required = Arrays.asList(
                "location_id","scan_location","operator_id","device_id",
                "epc_code","epc_header","epc_lot","epc_serial",
                "epc_product","epc_company","product_name",
                "event_time","business_step","event_type","hub_type"
        );
        List<String> missing = required.stream()
                .filter(r -> !colIdx.containsKey(r))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new InvalidCsvFormatException("누락된 헤더: " + missing);
        }

        return colIdx;
    }

    // [빈값 및 null 확인] : 주어진 행(row)에서 컬럼명에 해당하는 값을 안전하게 읽어와 null/빈값을 처리
    private String getValue(Map<String, Integer> colIdx, String[] row, String colName) {
        Integer idx = colIdx.get(colName.toLowerCase());
        if (idx == null || idx >= row.length) return null;
        String v = row[idx];
        return (v == null || v.isBlank()) ? null : v.trim();
        // 필요시 "NULL"/"null" 같은 문자열도 무시하도록 확장 가능
    }

    // 문자열을 Long으로 변환하되 포맷오류 발생 시 null을 반환하는 안전 변환기
    private Long parseLongSafe(String s) {
        if (s == null) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // [날짜 + 시간 변환] : 문자열을 LocalDateTime으로 파싱하고 실패 시 errorRows에 행번호를 기록한 뒤 null을 반환
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

    // [날짜 변환] : 문자열을 LocalDate으로 파싱하고 실패 시 errorRows에 행번호를 기록한 뒤 null을 반환
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

    // [business_step 변환] : 입력된 business_step 문자열을 내부 표준(Factory, WMS, LogiHub 등)으로 정규화
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

    // EventHistory의 유니크 제약을 만족하는 문자열 키(파일ID·EPC·로케이션·제품·시간·스텝·이벤트타입 조합)를 생성
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
                product != null ? String.valueOf(product.getCsvProductId()) : "null",
                epoch,
                businessStep != null ? businessStep : "null",
                eventType != null ? eventType : "null"
        );
    }
    // [Hash64] : FNV-1a 알고리즘으로 주어진 문자열의 64비트 해시값을 계산하여 반환
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
    // 청크 처리 중 사용하는 인메모리 캐시(locationIds, productMap, epcMap, epcCodes)를 초기화
    @RequiredArgsConstructor
    private static final class ImportCache {
        final Set<Long> locationIds;              // 전역 locationId (CsvLocation PK) 중복 체크
        final Map<String, CsvProduct> productMap; // 파일 내 product 키 -> CsvProduct
        final Map<String, Epc> epcMap;            // 파일 내 epcCode -> Epc
        final Set<String> epcCodes;               // 파일 내 epcCode 중복 체크
    }

}
