package edu.pnu.service;

public interface StatisticsInterface {
    String getProcessorName();

    int getOrder();

    void process(Long fileId);   // 집계 실행
}