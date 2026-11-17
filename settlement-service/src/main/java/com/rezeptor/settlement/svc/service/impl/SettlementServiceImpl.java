package com.rezeptor.settlement.svc.service.impl;

import com.rezeptor.settlement.svc.client.ExpenseServiceClient;
import com.rezeptor.settlement.svc.data.BalanceData;
import com.rezeptor.settlement.svc.data.TransferData;
import com.rezeptor.settlement.svc.dto.response.BalanceDto;
import com.rezeptor.settlement.svc.dto.response.SettlementSuggestionDto;
import com.rezeptor.settlement.svc.dto.response.TransferDto;
import com.rezeptor.settlement.svc.repository.SettlementRepository;
import com.rezeptor.settlement.svc.service.DebtSimplifierService;
import com.rezeptor.settlement.svc.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {
    private final SettlementRepository settlementRepository;
    private final ExpenseServiceClient expenseServiceClient;
    private final DebtSimplifierService debtSimplifierService;

    @Override
    @Transactional(readOnly = true)
    public SettlementSuggestionDto getSuggestion(Long groupId) {
        List<BalanceData> finalBalances = calculateFinalBalances(groupId);
        List<TransferData> transfers = debtSimplifierService.simplify(finalBalances);

        return SettlementSuggestionDto.builder()
                .groupId(groupId)
                .finalBalances(finalBalances.stream()
                        .map(balance -> BalanceDto.builder()
                                .userId(balance.getUserId())
                                .balance(balance.getAmount())
                                .build())
                        .toList())
                .transfers(transfers.stream()
                        .map(transfer -> TransferDto.builder()
                                .fromUserId(transfer.getFromUserId())
                                .toUserId(transfer.getToUserId())
                                .amount(transfer.getAmount())
                                .build())
                        .toList())
                .transferCount(transfers.size())
                .build();
    }

    private List<BalanceData> calculateFinalBalances(Long groupId) {
        Stream<BalanceData> expenseBalances = expenseServiceClient.getBalances(groupId).stream();
        Stream<BalanceData> settledBalances = settlementRepository.findNetSettledByGroupId(groupId).stream()
                .map(settled -> new BalanceData(settled.getUserId(), settled.getNetSettled()));

        return Stream.concat(expenseBalances, settledBalances)
                .collect(Collectors.toMap(BalanceData::getUserId, BalanceData::getAmount, BigDecimal::add))
                .entrySet().stream()
                .map(entry -> new BalanceData(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(BalanceData::getAmount).reversed()
                        .thenComparing(BalanceData::getUserId))
                .toList();
    }
}
