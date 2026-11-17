package com.rezeptor.expense.svc.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantReq {
    @NotNull(message = "userId must not be null")
    @Positive(message = "userId must be greater than zero")
    private Long userId;

    @Positive(message = "value must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "value must have at most 2 decimal places")
    private BigDecimal value;
}
