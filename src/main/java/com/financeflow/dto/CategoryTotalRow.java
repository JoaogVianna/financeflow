package com.financeflow.dto;

import com.financeflow.entity.TransactionType;

import java.math.BigDecimal;

/** Uma linha do resultado da consulta agrupada: total de um tipo dentro de uma categoria. */
public record CategoryTotalRow(Long categoryId, String categoryName, TransactionType type, BigDecimal total) {
}
