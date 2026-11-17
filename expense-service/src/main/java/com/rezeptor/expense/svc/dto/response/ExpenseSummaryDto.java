package com.rezeptor.expense.svc.dto.response;

import com.rezeptor.expense.svc.constant.Category;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@Builder
public class ExpenseSummaryDto {
    private Long id;
    private String description;
    private Category category;
    private BigDecimal amount;
    private Long paidBy;
    private LocalDate expenseDate;
    private Long participantCount;
}
