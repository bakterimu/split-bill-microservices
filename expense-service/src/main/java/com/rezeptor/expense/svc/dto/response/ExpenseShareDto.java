package com.rezeptor.expense.svc.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class ExpenseShareDto {
    private Long userId;
    private BigDecimal shareAmount;
}
