package com.rezeptor.expense.svc.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ShareResultData {
    private Long userId;
    private BigDecimal shareAmount;
}
