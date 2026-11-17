package com.rezeptor.expense.svc.service;

import com.rezeptor.expense.svc.dto.request.ExpenseCreateReq;
import com.rezeptor.expense.svc.dto.response.BalanceDto;
import com.rezeptor.expense.svc.dto.response.ExpenseDto;
import com.rezeptor.expense.svc.dto.response.ExpenseSummaryDto;
import com.rezeptor.expense.svc.dto.response.PageDto;

import java.util.List;

public interface ExpenseService {
    ExpenseDto createExpense(ExpenseCreateReq req);
    ExpenseDto getExpenseById(Long expenseId);
    PageDto<ExpenseSummaryDto> getExpensesByGroup(Long groupId, int page, int size);
    List<BalanceDto> getBalancesByGroup(Long groupId);
}
