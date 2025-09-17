package edu.pnu.service.analysis.statistics.api;

public interface StatisticsInterface {
    String getProcessorName();

    int getOrder();

    void process(Long fileId);   // 집계 실행
}