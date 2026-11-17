package com.rezeptor.settlement.svc.entity.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SettlementHistoryProjection {
    Long getId();
    Long getFromUserId();
    Long getToUserId();
    BigDecimal getAmount();
    String getNote();
    LocalDateTime getPaidAt();
    BigDecimal getCumulativeAmount();
}
