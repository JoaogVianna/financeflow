package com.financeflow.service;

import com.financeflow.dto.BalanceResponse;
import com.financeflow.dto.TransactionRequest;
import com.financeflow.dto.TransactionResponse;
import com.financeflow.entity.Category;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.TransactionType;
import com.financeflow.exception.InvalidRequestException;
import com.financeflow.exception.ResourceNotFoundException;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    // Quando o período não é informado, consideramos todo o histórico.
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public TransactionResponse create(TransactionRequest request) {
        Transaction transaction = new Transaction();
        apply(transaction, request);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    // @Transactional mantém a conexão aberta pra carregar a categoria (carregamento "lazy").
    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return TransactionResponse.from(getTransaction(id));
    }

    public List<TransactionResponse> findAll(LocalDate startDate, LocalDate endDate, Long categoryId) {
        LocalDate start = startDate != null ? startDate : MIN_DATE;
        LocalDate end = endDate != null ? endDate : MAX_DATE;
        validatePeriod(start, end);

        List<Transaction> transactions = categoryId == null
                ? transactionRepository.findByPeriod(start, end)
                : transactionRepository.findByPeriodAndCategory(start, end, categoryId);

        return transactions.stream().map(TransactionResponse::from).toList();
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = getTransaction(id);
        apply(transaction, request);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long id) {
        transactionRepository.delete(getTransaction(id));
    }

    // Saldo = total de receitas - total de despesas no período.
    public BalanceResponse balance(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : MIN_DATE;
        LocalDate end = endDate != null ? endDate : MAX_DATE;
        validatePeriod(start, end);

        BigDecimal income = orZero(transactionRepository.sumByTypeAndPeriod(TransactionType.INCOME, start, end));
        BigDecimal expense = orZero(transactionRepository.sumByTypeAndPeriod(TransactionType.EXPENSE, start, end));

        return new BalanceResponse(startDate, endDate, income, expense, income.subtract(expense));
    }

    private void apply(Transaction transaction, TransactionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada: " + request.categoryId()));
        transaction.setDescription(request.description().trim());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setDate(request.date());
        transaction.setCategory(category);
    }

    private Transaction getTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada: " + id));
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new InvalidRequestException("A data inicial não pode ser depois da data final");
        }
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
