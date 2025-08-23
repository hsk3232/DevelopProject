package edu.pnu.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import edu.pnu.domain.Csv;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public abstract class CsvFileDTO {


	// ■■■■■■■■■■■■ [ REQUEST DTO ] ■■■■■■■■■■■■ //

	// [csv 정보 기록] : 업로드한 csv 파일 정보 기록
	// 기존 CsvFileListResponseDTO
	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class FileInfo {
		private Long fileId;
		private String fileName;
		private String userId;
		private Long fileSize;
		private LocalDateTime createAt;

		public static FileInfo from(Csv c) {
			return FileInfo.builder()
					.fileId(c.getFileId())
					.fileName(c.getFileName())
					.userId(c.getMember().getUserId())
					.fileSize(c.getFileSize())
					.createAt(c.getCreatedAt())
					.build();
		}
	}



	// ■■■■■■■■■■■■ [ RESPONSE DTO ] ■■■■■■■■■■■■ //
	
	// 목록 전체를 감싸는 Wrapper
	@Getter
	@Setter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class FileListResponse {
		private List<FileInfo> data;
		private Long nextCursor;
		
		public static FileListResponse from(List<Csv> csvList, Long nextCursor) {
			List<FileInfo> fileInfos = csvList.stream()
					.map(FileInfo::from)
					.collect(Collectors.toList());
			
			return new FileListResponse(fileInfos, nextCursor);
		}
	}

}
