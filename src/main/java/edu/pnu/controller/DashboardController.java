package edu.pnu.controller;

import edu.pnu.dto.DashboardDTO;
import edu.pnu.exception.NoDataFoundException;
import edu.pnu.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/manager")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/kpi")
    public DashboardDTO.KpiResponse getKPIAnalysis(@RequestParam Long fileId) {
        log.info("[진입] : [DashboardController] Kpi 정보 load 진입");
        DashboardDTO.KpiResponse dto = dashboardService.getKPIAnalysis(fileId);
        if(dto == null) {
            throw new NoDataFoundException("[오류] : [DashboardController] KPI 조회 List가 비었음.");
        }
        log.info("[성공] : [DashboardController] KPI 정보 Load 성공");
        return dto;
    }
}
