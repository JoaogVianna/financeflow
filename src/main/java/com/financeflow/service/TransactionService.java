package com.financeflow.service;

import com.financeflow.dto.BalanceResponse;
import com.financeflow.dto.TransactionRequest;
import com.financeflow.dto.TransactionResponse;
import com.financeflow.entity.Category;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.TransactionType;
import com.financeflow.entity.User;
import com.financeflow.exception.InvalidRequestException;
import com.financeflow.exception.ResourceNotFoundException;
import com.financeflow.exception.UnauthorizedException;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
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
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public TransactionResponse create(Long userId, TransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        apply(userId, transaction, request);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    // @Transactional mantém a conexão aberta pra carregar a categoria (carregamento "lazy").
    @Transactional(readOnly = true)
    public TransactionResponse findById(Long userId, Long id) {
        return TransactionResponse.from(getTransaction(userId, id));
    }

    public List<TransactionResponse> findAll(Long userId, LocalDate startDate, LocalDate endDate, Long categoryId) {
        LocalDate start = startDate != null ? startDate : MIN_DATE;
        LocalDate end = endDate != null ? endDate : MAX_DATE;
        validatePeriod(start, end);

        List<Transaction> transactions = categoryId == null
                ? transactionRepository.findByPeriod(userId, start, end)
                : transactionRepository.findByPeriodAndCategory(userId, start, end, categoryId);

        return transactions.stream().map(TransactionResponse::from).toList();
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionRequest request) {
        Transaction transaction = getTransaction(userId, id);
        apply(userId, transaction, request);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        transactionRepository.delete(getTransaction(userId, id));
    }

    // Saldo = total de receitas - total de despesas do usuário no período.
    public BalanceResponse balance(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : MIN_DATE;
        LocalDate end = endDate != null ? endDate : MAX_DATE;
        validatePeriod(start, end);

        BigDecimal income = orZero(
                transactionRepository.sumByTypeAndPeriod(userId, TransactionType.INCOME, start, end));
        BigDecimal expense = orZero(
                transactionRepository.sumByTypeAndPeriod(userId, TransactionType.EXPENSE, start, end));

        return new BalanceResponse(startDate, endDate, income, expense, income.subtract(expense));
    }

    private void apply(Long userId, Transaction transaction, TransactionRequest request) {
        // A categoria precisa ser do próprio usuário; senão, é como se não existisse.
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada: " + request.categoryId()));
        transaction.setDescription(request.description().trim());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setDate(request.date());
        transaction.setCategory(category);
    }

    private Transaction getTransaction(Long userId, Long id) {
        return transactionRepository.findByIdAndUserId(id, userId)
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
