package com.rezeptor.expense.svc.dto.response;

import com.rezeptor.expense.svc.constant.Category;
import com.rezeptor.expense.svc.constant.SplitType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Builder
public class ExpenseDto {
    private Long id;
    private Long groupId;
    private Long paidBy;
    private String description;
    private Category category;
    private BigDecimal amount;
    private SplitType splitType;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private List<ExpenseShareDto> shares;
}
