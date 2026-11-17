package com.rezeptor.expense.svc.dto.request;

import com.rezeptor.expense.svc.constant.Category;
import com.rezeptor.expense.svc.constant.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCreateReq {
    @NotNull(message = "groupId must not be null")
    @Positive(message = "groupId must be greater than zero")
    private Long groupId;

    @NotNull(message = "paidBy must not be null")
    @Positive(message = "paidBy must be greater than zero")
    private Long paidBy;

    @NotBlank(message = "description must not be empty")
    @Size(max = 255, message = "description must be at most 255 characters")
    private String description;

    @NotNull(message = "category must not be null")
    private Category category;

    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "expenseDate must not be null")
    private LocalDate expenseDate;

    @NotNull(message = "splitType must not be null")
    private SplitType splitType;

    @Valid
    @NotEmpty(message = "participants must not be empty")
    private List<ParticipantReq> participants;
}
