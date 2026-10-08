package com.financeflow.dto;

import com.financeflow.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres")
        String description,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 13, fraction = 2, message = "Valor inválido (máximo 2 casas decimais)")
        BigDecimal amount,

        @NotNull(message = "O tipo é obrigatório (INCOME ou EXPENSE)")
        TransactionType type,

        @NotNull(message = "A data é obrigatória")
        LocalDate date,

        @NotNull(message = "A categoria é obrigatória")
        Long categoryId
) {
}
