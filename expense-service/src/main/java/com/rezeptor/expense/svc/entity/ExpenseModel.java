package com.rezeptor.expense.svc.entity;

import com.rezeptor.expense.svc.constant.Category;
import com.rezeptor.expense.svc.constant.SplitType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private Long paidBy;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SplitType splitType;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseShareModel> shares = new ArrayList<>();

    public void addShare(Long userId, BigDecimal shareAmount) {
        shares.add(new ExpenseShareModel(this, userId, shareAmount));
    }
}
