package com.rezeptor.expense.svc.entity.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseSummaryProjection {
    Long getId();
    String getDescription();
    String getCategory();
    BigDecimal getAmount();
    Long getPaidBy();
    LocalDate getExpenseDate();
    Long getParticipantCount();
}
