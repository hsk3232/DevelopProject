package edu.pnu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class DashboardDTO {

//	 ■■■■■■■■■■■■■■ [ 중첩 클래스 ] ■■■■■■■■■■■■■ 
    // 다른 Response DTO에서 재사용되는 내부 클래스

    // 기존 AnalyzedTripDTO의 내부 클래스 → AnalyzedTripResponse에서 객체로 사용
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class TripPoint {
        private String scanLocation;
        private List<Double> coord;
        private Long eventTime;
        private String businessStep;
    }

    // 기존 InventoryDTO → InventoryListResponse에서 사용
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class InventoryItem {
        private String businessStep;
        private Long value;
    }

    // 기존 ByProductDTO → ByProductListResponse에서 사용
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ByProductInfo {
        private String productName;
        private Long fake;
        private Long tamper;
        private Long clone;
        private Long other;
        private Long total;
    }

    // 기존 NodeDTO → NodeListResponse에서 사용
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeInfo {
        private String hubType;
        private String scanLocation;
        private String businessStep;
        private List<Double> coord;
    }

    @Getter
    @AllArgsConstructor // 모든 필드를 받는 생성자 (JPA Constructor Expression이 사용)
    @ToString
    public static class TimeRange { // DTO 접미사를 빼서 값 객체임을 더 명확히 함
        private final LocalDateTime minTime; // 불변성을 위해 final 유지
        private final LocalDateTime maxTime;
    }


//	 ■■■■■■■■■■■■ [ RESPONSE DTO ] ■■■■■■■■■■■■ 

    // 기존 AnalyzedTripDTO 리스트 Wrapper
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripListResponse {
        private List<AnalyzedTrip> data;
        private Long nextCursor;
        private Integer pageSize;
        private Boolean hasNext;


        @Getter
        @Setter
        @ToString
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class AnalyzedTrip {
            private TripPoint from;
            private TripPoint to;
            private Long fileId;
            private String epcCode;
            private String productName;
            private String epcLot;
            private Long roadId;
            @Builder.Default
            private List<String> anomalyTypeList = new ArrayList<>();

            // JPA Constructor Expression 위한 생성자
            // JPA 조회 결과에 포함되지 않는 필드(from, to, anomalyTypeList 등)이 DTO에 존재하기 때문에
            // @AllArgsConstructor 사용 안됨
            public AnalyzedTrip(String fromScanLocation, Double fromLongitude, Double fromLatitude,
                                LocalDateTime fromEventTime, String fromBusinessStep, String toScanLocation, Double toLongitude,
                                Double toLatitude, LocalDateTime toEventTime, String toBusinessStep, Long fileId, String epcCode,
                                String productName, String epcLot, Long roadId) {

                this.from = TripPoint.builder().scanLocation(fromScanLocation)
                        .coord(fromLongitude != null && fromLatitude != null ? List.of(fromLongitude, fromLatitude)
                                : new ArrayList<>())
                        .eventTime(fromEventTime != null ? fromEventTime.toEpochSecond(ZoneOffset.UTC) : null)
                        .businessStep(fromBusinessStep).build();

                this.to = TripPoint.builder().scanLocation(toScanLocation)
                        .coord(toLongitude != null && toLatitude != null ? List.of(toLongitude, toLatitude)
                                : new ArrayList<>())
                        .eventTime(toEventTime != null ? toEventTime.toEpochSecond(ZoneOffset.UTC) : null)
                        .businessStep(toBusinessStep).build();

                this.fileId = fileId;
                this.epcCode = epcCode;
                this.productName = productName;
                this.epcLot = epcLot;
                this.roadId = roadId;
                this.anomalyTypeList = new ArrayList<>();
            }
        }
    }

    // 대시보드 필터 옵션 목록 응답을 위한 DTO
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterOptionsResponse {
        private List<String> scanLocations;
        // eventTimeRange는 서비스 레이어에서 TimeRange 객체를 받아 가공하여 채워짐
        private List<LocalDateTime> eventTimeRange;
        private List<String> businessSteps;
        private List<String> productNames;
        private List<String> eventTypes;
        private List<String> anomalyTypes;
    }

    // KPI(핵심 성과 지표) 조회 응답을 위한 DTO
    // 기존 KPIExportDTO
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpiResponse {
        private Long kpiId;
        private Long anomalyCount;
        private double anomalyRate;
        private double avgLeadTime;
        private Long codeCount;
        private double dispatchRate;
        private double inventoryRate;
        private double salesRate;
        private Long totalTripCount;
        private int uniqueProductCount;
        private Long fileId;
    }

    // 재고 분포 현황 목록 응답을 위한 DTO
    // 기존 InventoryDTO 리스트 Wrapper
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryListResponse {
        private List<InventoryItem> inventoryDistribution;
    }

    // 기존 ByProductDTO 리스트 Wrapper
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ByProductListResponse {
        private List<ByProductInfo> byProductList;
    }

    // 기존 NodeDTO 리스트 Wrapper
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeListResponse {
        private List<NodeInfo> nodes;
    }

    // [From 목록] : 출발지 기준 도착지 목록 응답을 위한 DTO
    // 기존 FromToDTO
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToLocationResponse {
        private List<String> toLocation;
    }


}
