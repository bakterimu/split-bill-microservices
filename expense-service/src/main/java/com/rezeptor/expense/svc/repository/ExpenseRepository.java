package com.rezeptor.expense.svc.repository;

import com.rezeptor.expense.svc.entity.ExpenseModel;
import com.rezeptor.expense.svc.entity.projection.BalanceProjection;
import com.rezeptor.expense.svc.entity.projection.ExpenseSummaryProjection;
import com.rezeptor.expense.svc.entity.projection.MonthlyReportProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseModel, Long> {

    @Query(value = """
            select e.id           as id,
                   e.description  as description,
                   e.category     as category,
                   e.amount       as amount,
                   e.paid_by      as paidBy,
                   e.expense_date as expenseDate,
                   count(s.id)    as participantCount
            from expenses e
            join expense_shares s on s.expense_id = e.id
            where e.group_id = :groupId
            group by e.id
            order by e.expense_date desc, e.id desc
            """,
            countQuery = """
            select count(*)
            from expenses e
            where e.group_id = :groupId
            """,
            nativeQuery = true)
    Page<ExpenseSummaryProjection> findSummariesByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query(value = """
            with paid as (
                select paid_by as user_id, sum(amount) as total_paid
                from expenses
                where group_id = :groupId
                group by paid_by
            ),
            owed as (
                select s.user_id, sum(s.share_amount) as total_owed
                from expense_shares s
                join expenses e on e.id = s.expense_id
                where e.group_id = :groupId
                group by s.user_id
            )
            select coalesce(p.user_id, o.user_id)                        as userId,
                   coalesce(p.total_paid, 0)                             as totalPaid,
                   coalesce(o.total_owed, 0)                             as totalOwed,
                   coalesce(p.total_paid, 0) - coalesce(o.total_owed, 0) as netBalance
            from paid p
            full outer join owed o on o.user_id = p.user_id
            order by netBalance desc, userId
            """, nativeQuery = true)
    List<BalanceProjection> findBalancesByGroupId(@Param("groupId") Long groupId);

    @Query(value = """
            select to_char(date_trunc('month', e.expense_date), 'YYYY-MM') as month,
                   e.category                                              as category,
                   count(*)                                                as transactionCount,
                   sum(e.amount)                                           as totalAmount,
                   round(avg(e.amount), 2)                                 as averageAmount
            from expenses e
            where e.group_id = :groupId
              and e.expense_date >= make_date(:year, 1, 1)
              and e.expense_date <  make_date(:year + 1, 1, 1)
            group by date_trunc('month', e.expense_date), e.category
            having sum(e.amount) >= :minTotal
            order by month, totalAmount desc
            """, nativeQuery = true)
    List<MonthlyReportProjection> findMonthlyReport(@Param("groupId") Long groupId,
                                                    @Param("year") int year,
                                                    @Param("minTotal") BigDecimal minTotal);
}
