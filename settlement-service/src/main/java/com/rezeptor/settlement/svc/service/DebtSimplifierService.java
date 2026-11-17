package com.rezeptor.settlement.svc.service;

import com.rezeptor.settlement.svc.data.BalanceData;
import com.rezeptor.settlement.svc.data.TransferData;

import java.util.List;

public interface DebtSimplifierService {
    List<TransferData> simplify(List<BalanceData> balances);
}
