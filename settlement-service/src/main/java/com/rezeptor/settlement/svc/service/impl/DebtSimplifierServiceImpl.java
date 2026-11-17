package com.rezeptor.settlement.svc.service.impl;

import com.rezeptor.settlement.svc.data.BalanceData;
import com.rezeptor.settlement.svc.data.TransferData;
import com.rezeptor.settlement.svc.service.DebtSimplifierService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DebtSimplifierServiceImpl implements DebtSimplifierService {

    @Override
    public List<TransferData> simplify(List<BalanceData> balances) {
        BigDecimal total = balances.stream()
                .map(BalanceData::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0) {
            throw new IllegalStateException("Balances must sum to zero, but was " + total.toPlainString());
        }

        Map<Boolean, List<BalanceData>> partitioned = balances.stream()
                .filter(balance -> balance.getAmount().signum() != 0)
                .collect(Collectors.partitioningBy(balance -> balance.getAmount().signum() > 0));

        List<BalanceData> creditors = sortByLargestAmount(partitioned.get(true));
        List<BalanceData> debtors = sortByLargestAmount(partitioned.get(false));

        List<TransferData> transfers = new ArrayList<>();
        int creditorIndex = 0;
        int debtorIndex = 0;
        BigDecimal remainingCredit = creditors.isEmpty() ? BigDecimal.ZERO : creditors.get(0).getAmount();
        BigDecimal remainingDebt = debtors.isEmpty() ? BigDecimal.ZERO : debtors.get(0).getAmount();

        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            BigDecimal amount = remainingCredit.min(remainingDebt);
            transfers.add(new TransferData(
                    debtors.get(debtorIndex).getUserId(),
                    creditors.get(creditorIndex).getUserId(),
                    amount));

            remainingCredit = remainingCredit.subtract(amount);
            remainingDebt = remainingDebt.subtract(amount);

            if (remainingCredit.signum() == 0 && ++creditorIndex < creditors.size()) {
                remainingCredit = creditors.get(creditorIndex).getAmount();
            }
            if (remainingDebt.signum() == 0 && ++debtorIndex < debtors.size()) {
                remainingDebt = debtors.get(debtorIndex).getAmount();
            }
        }

        return transfers;
    }

    private List<BalanceData> sortByLargestAmount(List<BalanceData> balances) {
        return balances.stream()
                .map(balance -> new BalanceData(balance.getUserId(), balance.getAmount().abs()))
                .sorted(Comparator.comparing(BalanceData::getAmount).reversed()
                        .thenComparing(BalanceData::getUserId))
                .toList();
    }
}
