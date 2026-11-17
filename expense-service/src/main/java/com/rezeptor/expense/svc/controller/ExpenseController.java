package com.rezeptor.expense.svc.controller;

import com.rezeptor.expense.svc.dto.request.ExpenseCreateReq;
import com.rezeptor.expense.svc.dto.response.ExpenseDto;
import com.rezeptor.expense.svc.dto.response.Response;
import com.rezeptor.expense.svc.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expense")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<Response<ExpenseDto>> createExpense(@Valid @RequestBody ExpenseCreateReq req) {
        return Response.created(expenseService.createExpense(req)).toEntity();
    }
}
