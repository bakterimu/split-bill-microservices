package com.rezeptor.expense.svc.entity.projection;

import java.math.BigDecimal;

public interface MonthlyReportProjection {
    String getMonth();
    String getCategory();
    Long getTransactionCount();
    BigDecimal getTotalAmount();
    BigDecimal getAverageAmount();
}
