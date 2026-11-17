package com.rezeptor.settlement.svc.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class TransferDto {
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
}
