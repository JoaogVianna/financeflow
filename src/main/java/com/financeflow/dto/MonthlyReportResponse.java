package com.financeflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyReportResponse(
        int year,
        int month,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<CategorySummary> categories
) {
}
