package edu.pnu.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AiCommunicationDTO {

//	 ■■■■■■■■■■■■ Export (BE -> AI) ■■■■■■■■■■■■

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class EventData { // 기존 ExportEventHistoryDTO
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
	public static class ExportRequest { // 기존 ExportDataToAiDTO
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
