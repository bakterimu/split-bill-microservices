package com.rezeptor.expense.svc.entity.projection;

import java.math.BigDecimal;

public interface BalanceProjection {
    Long getUserId();
    BigDecimal getTotalPaid();
    BigDecimal getTotalOwed();
    BigDecimal getNetBalance();
}
