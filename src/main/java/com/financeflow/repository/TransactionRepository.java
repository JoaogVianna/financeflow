package com.financeflow.repository;

import com.financeflow.entity.Transaction;
import com.financeflow.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select t from Transaction t join fetch t.category
            where t.date between :startDate and :endDate
            order by t.date desc, t.id desc
            """)
    List<Transaction> findByPeriod(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    @Query("""
            select t from Transaction t join fetch t.category
            where t.date between :startDate and :endDate
              and t.category.id = :categoryId
            order by t.date desc, t.id desc
            """)
    List<Transaction> findByPeriodAndCategory(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("categoryId") Long categoryId);

    @Query("""
            select sum(t.amount) from Transaction t
            where t.type = :transactionType
              and t.date between :startDate and :endDate
            """)
    BigDecimal sumByTypeAndPeriod(@Param("transactionType") TransactionType transactionType,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}
