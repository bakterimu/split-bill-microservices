package com.rezeptor.expense.svc.service;

import com.rezeptor.expense.svc.dto.request.ExpenseCreateReq;
import com.rezeptor.expense.svc.dto.response.ExpenseDto;

public interface ExpenseService {
    ExpenseDto createExpense(ExpenseCreateReq req);
}
