package com.rezeptor.settlement.svc.entity.projection;

import java.math.BigDecimal;

public interface SettledBalanceProjection {
    Long getUserId();
    BigDecimal getNetSettled();
}
