package com.rezeptor.expense.svc.controller;

import com.rezeptor.expense.svc.dto.request.ExpenseCreateReq;
import com.rezeptor.expense.svc.dto.response.BalanceDto;
import com.rezeptor.expense.svc.dto.response.ExpenseDto;
import com.rezeptor.expense.svc.dto.response.ExpenseSummaryDto;
import com.rezeptor.expense.svc.dto.response.MonthlyReportDto;
import com.rezeptor.expense.svc.dto.response.PageDto;
import com.rezeptor.expense.svc.dto.response.Response;
import com.rezeptor.expense.svc.service.ExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/expense")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<Response<ExpenseDto>> createExpense(@Valid @RequestBody ExpenseCreateReq req) {
        return Response.created(expenseService.createExpense(req)).toEntity();
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<Response<ExpenseDto>> getExpenseById(@PathVariable @Positive Long expenseId) {
        return Response.ok(expenseService.getExpenseById(expenseId)).toEntity();
    }

    @GetMapping
    public ResponseEntity<Response<PageDto<ExpenseSummaryDto>>> getExpensesByGroup(
            @RequestParam @Positive Long groupId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return Response.ok(expenseService.getExpensesByGroup(groupId, page, size)).toEntity();
    }

    @GetMapping("/balance")
    public ResponseEntity<Response<List<BalanceDto>>> getBalancesByGroup(@RequestParam @Positive Long groupId) {
        return Response.ok(expenseService.getBalancesByGroup(groupId)).toEntity();
    }

    @GetMapping("/report/monthly")
    public ResponseEntity<Response<List<MonthlyReportDto>>> getMonthlyReport(
            @RequestParam @Positive Long groupId,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(defaultValue = "0") @PositiveOrZero BigDecimal minTotal) {
        return Response.ok(expenseService.getMonthlyReport(groupId, year, minTotal)).toEntity();
    }
}
