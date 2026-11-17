package com.rezeptor.settlement.svc.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCreateReq {
    @NotNull(message = "groupId must not be null")
    @Positive(message = "groupId must be greater than zero")
    private Long groupId;

    @NotNull(message = "fromUserId must not be null")
    @Positive(message = "fromUserId must be greater than zero")
    private Long fromUserId;

    @NotNull(message = "toUserId must not be null")
    @Positive(message = "toUserId must be greater than zero")
    private Long toUserId;

    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;

    @Size(max = 255, message = "note must be at most 255 characters")
    private String note;
}
