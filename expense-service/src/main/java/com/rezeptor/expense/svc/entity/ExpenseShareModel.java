package com.rezeptor.expense.svc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_shares")
@Setter
@Getter
@NoArgsConstructor
public class ExpenseShareModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private ExpenseModel expense;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal shareAmount;

    public ExpenseShareModel(ExpenseModel expense, Long userId, BigDecimal shareAmount) {
        this.expense = expense;
        this.userId = userId;
        this.shareAmount = shareAmount;
    }
}
