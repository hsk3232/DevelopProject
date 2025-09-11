package edu.pnu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KPIAnalysisComponet implements StatisticsInterface {
    @Override
    public String getProcessorName() {
        return "KPI 분석";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void process(Long fileId) {
    }
}
