package com.financeflow.dto;

import java.math.BigDecimal;

public record CategorySummary(
        Long categoryId,
        String categoryName,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance
) {
}
