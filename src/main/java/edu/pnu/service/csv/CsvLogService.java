package edu.pnu.service.csv;

import edu.pnu.domain.CsvFile;
import edu.pnu.dto.CsvFileDTO;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.repository.CsvRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvLogService {
    private final CsvRouteRepository csvRepository;


    @Transactional(readOnly = true)
    public CsvFileDTO.FileListResponse getFileListByCursor(Long cursor, int size, String search, Long locationId) {

        // 커서 기반 페이징에서는 항상 첫 번째 페이지(0)를 조회
        PageRequest pageable = PageRequest.of(0, size);


        // findCsvListByCriteria 메서드를 호출하여 List<Csv> 타입의 csvList 변수에 결과를 할당
        List<CsvFile> csvList = csvRepository.findCsvListByCriteria(locationId, cursor, search, pageable);

        Long nextCursor = null;
        if (!csvList.isEmpty() && csvList.size() == size) {
            nextCursor = csvList.get(csvList.size() - 1).getFileId();
        }

        // CsvFileDTO의 정적 팩토리 메서드를 사용하여 최종 응답 DTO를 생성
        return CsvFileDTO.FileListResponse.from(csvList, nextCursor);
    }

    // 업로한 CSV 파일 재다운 로드
    public Resource loadCsvResource(Long fileId) {
        CsvFile csv = findCsvById(fileId);
        try {
            Path filePath = Paths.get(csv.getFilePath()).resolve(csv.getSavedFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new CsvFileNotFoundException("저장된 파일을 읽을 수 없습니다: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new CsvFileNotFoundException("파일 경로가 유효하지 않습니다: " + e.getMessage());
        }
    }

    // 저장된 파일 이름 검색
    public String getOriginalFilename(Long fileId) {
        return findCsvById(fileId).getFileName();
    }

    // file ID 검색
    private CsvFile findCsvById(Long fileId) {
        return csvRepository.findById(fileId)
                .orElseThrow(() -> new CsvFileNotFoundException("요청된 파일 ID를 찾을 수 없습니다: " + fileId));
    }
}
