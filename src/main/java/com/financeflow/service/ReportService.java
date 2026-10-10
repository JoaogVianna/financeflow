package com.financeflow.service;

import com.financeflow.dto.CategorySummary;
import com.financeflow.dto.CategoryTotalRow;
import com.financeflow.dto.MonthlyReportResponse;
import com.financeflow.entity.TransactionType;
import com.financeflow.exception.InvalidRequestException;
import com.financeflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public MonthlyReportResponse monthly(Long userId, Integer year, Integer month) {
        YearMonth now = YearMonth.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();

        if (m < 1 || m > 12) {
            throw new InvalidRequestException("O mês deve estar entre 1 e 12");
        }
        if (y < 1900 || y > 9999) {
            throw new InvalidRequestException("Ano inválido");
        }

        YearMonth yearMonth = YearMonth.of(y, m);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // O banco devolve uma linha por (categoria, tipo); aqui juntamos receita e despesa
        // da mesma categoria numa linha só.
        Map<Long, CategoryAcc> byCategory = new LinkedHashMap<>();
        for (CategoryTotalRow row : transactionRepository.totalsByCategory(userId, start, end)) {
            CategoryAcc acc = byCategory.computeIfAbsent(
                    row.categoryId(), id -> new CategoryAcc(id, row.categoryName()));
            if (row.type() == TransactionType.INCOME) {
                acc.income = row.total();
            } else {
                acc.expense = row.total();
            }
        }

        List<CategorySummary> categories = byCategory.values().stream()
                .map(a -> new CategorySummary(a.id, a.name, a.income, a.expense, a.income.subtract(a.expense)))
                .toList();

        BigDecimal totalIncome = categories.stream()
                .map(CategorySummary::income).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = categories.stream()
                .map(CategorySummary::expense).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MonthlyReportResponse(y, m, start, end,
                totalIncome, totalExpense, totalIncome.subtract(totalExpense), categories);
    }

    private static class CategoryAcc {
        final Long id;
        final String name;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        CategoryAcc(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
