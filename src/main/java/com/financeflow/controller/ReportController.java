package com.financeflow.controller;

import com.financeflow.dto.MonthlyReportResponse;
import com.financeflow.security.CurrentUser;
import com.financeflow.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Sem year/month, usa o mês atual.
    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month) {
        return reportService.monthly(CurrentUser.id(jwt), year, month);
    }
}
