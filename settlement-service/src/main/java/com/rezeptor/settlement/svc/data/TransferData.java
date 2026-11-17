package com.rezeptor.settlement.svc.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TransferData {
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
}
