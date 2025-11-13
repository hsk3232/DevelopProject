package edu.pnu.dto;

import edu.pnu.domain.AnalysisSummary;
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
            // JPA 조회 결과에 포함되지 않는 필드(fromEntity, to, anomalyTypeList 등)이 DTO에 존재하기 때문에
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
        // 필터 조건 내에서 발견된 총 이상 징후 발생 건수n
        private Long anomalyCount; // ex) 125

        // 이상 발생 비율 (anomalyCount / (totalTripCount*5))
        // 프론트에서 100 곱해 %로 표시
        private double anomalyRate; // ex) 0.0146

        // 상품이 생산 시작부터 최종 판매까지 걸리는 평균 소요 시간 (day 단위)
        private double avgLeadTime; // ex) 12.5

        // 총 생산량 (생산된 epc 코드의 수)
        private Long codeCount; // ex) 900000

        // 생산량 대비 실제 출고된 상품의 비율(%)
        // 창고_out && epc_code 카디널리티 / 창고_in && epc_code 카디널리티
        private double dispatchRate; // ex) 95.1

        // 전체 보관 가능 용량 대비 현재 보관 중인 재고의 비율(%)
        // 각 단계 out / 각 단계 in
        private double inventoryRate; // ex) 78.2


        // 전체 입고량 대비 실제 판매(pos_Sell)된 상품의 비율(%)
        // pos_Sell && epc_code 카디널리티 / factory && epc_code 카디널리티
        private double salesRate; // ex) 92.5

        // 필터 조건에 맞는 전체 이동(Trip) 건수 (EventHistory의 row 수, 이동 event의 카디널리티)
        private Long totalTripCount; // ex) 854320

        // 필터 조건 내 고유한 상품(productName)의 총 종류 수
        // (ex: 각 공장별 생산 상품 종류, epc_product 기준)
        private int uniqueProductCount; // ex) 128

        private Long fileId;

        public static KpiResponse fromEntity(AnalysisSummary k) {
            return DashboardDTO.KpiResponse.builder()
                    //TO_DO
                    .totalTripCount(k.getTotalTripCount())
                    .uniqueProductCount(k.getUniqueProductCount())
                    .codeCount(k.getCodeCount())
                    .anomalyCount(k.getAnomalyCount())
                    .anomalyRate(k.getAnomalyRate())
                    .salesRate(k.getSalesRate())
                    .dispatchRate(k.getDispatchRate())
                    .inventoryRate(k.getInventoryRate())
                    .avgLeadTime(k.getAvgLeadTime())
                    .fileId(k.getCsvFile().getFileId())
                    .kpiId(k.getKpiId())
                    .build();
        }
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
