package com.rezeptor.settlement.svc.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class BalanceDto {
    private Long userId;
    private BigDecimal balance;
}
