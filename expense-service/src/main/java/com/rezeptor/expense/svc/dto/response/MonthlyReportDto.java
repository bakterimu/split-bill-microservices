package com.rezeptor.expense.svc.dto.response;

import com.rezeptor.expense.svc.constant.Category;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class MonthlyReportDto {
    private String month;
    private Category category;
    private Long transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
}
