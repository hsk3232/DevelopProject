package edu.pnu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public class AiCommunicationDTO {

//	 ■■■■■■■■■■■■ Export (BE -> AI) ■■■■■■■■■■■■

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
    // 기존 ExportEventHistoryDTO
	public static class EventData {
		private Long eventId;
		private Long locationId;
		private String businessStep;
		private String eventType;
		private LocalDateTime eventTime;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
    // 기존 ExportDataToAiDTO
	public static class ExportRequest {
		private String epcCode;
		private List<EventData> events;
	}

//	 ■■■■■■■■■■■■ Import (AI -> BE) ■■■■■■■■■■■■

	// 기존 ImportAiDataDTO
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AnomalyEvent {
		private Long eventId;
		private Double anomalyScore;
	}

	// 기존 ImportDatafromAiDTO
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ImportPayload {
		private Long fileId;
		@JsonSetter(nulls = Nulls.AS_EMPTY) // null로 올 수 있음 → 빈 리스트로 수용
		private List<AnomalyEvent> eventHistory;
	}
}
