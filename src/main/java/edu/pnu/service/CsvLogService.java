package edu.pnu.service;

import edu.pnu.domain.Csv;
import edu.pnu.dto.CsvFileDTO;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.repository.CsvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvLogService {
    private final CsvRepository csvRepository;
    

    @Transactional(readOnly = true)
    public CsvFileDTO.FileListResponse getFileListByCursor(Long cursor, int size, String search, Long locationId) {
        PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "fileId"));

        List<Csv> csvList;
        // 검색어 유무와 커서 유무에 따라 적절한 Repository 메서드 호출 (Repository 수정 필요)
        if (search != null && !search.isBlank()) {
            csvList = (cursor == null)
                    ? csvRepository.findByMember_Location_LocationIdAndFileNameContainingIgnoreCaseOrderByFileIdDesc(locationId, search, pageable)
                    : csvRepository.findByMember_Location_LocationIdAndFileIdLessThanAndFileNameContainingIgnoreCaseOrderByFileIdDesc(locationId, cursor, search, pageable);
        } else {
            csvList = (cursor == null)
                    ? csvRepository.findByMember_Location_LocationIdOrderByFileIdDesc(locationId, pageable)
                    : csvRepository.findByMember_Location_LocationIdAndFileIdLessThanOrderByFileIdDesc(locationId, cursor, pageable);
        }

        Long nextCursor = null;
        if (!csvList.isEmpty() && csvList.size() == size) {
            nextCursor = csvList.get(csvList.size() - 1).getFileId();
        }

        return CsvFileDTO.FileListResponse.from(csvList, nextCursor);
    }

    public Resource loadCsvResource(Long fileId) {
        Csv csv = findCsvById(fileId);
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

    public String getOriginalFilename(Long fileId) {
        return findCsvById(fileId).getFileName();
    }

    private Csv findCsvById(Long fileId) {
        return csvRepository.findById(fileId)
                .orElseThrow(() -> new CsvFileNotFoundException("요청된 파일 ID를 찾을 수 없습니다: " + fileId));
    }
}
