package com.rezeptor.settlement.svc.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class SettlementHistoryDto {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private String note;
    private LocalDateTime paidAt;
    private BigDecimal cumulativeAmount;
}
