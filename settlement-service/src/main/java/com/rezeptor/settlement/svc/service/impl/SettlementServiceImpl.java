package com.rezeptor.settlement.svc.service.impl;

import com.rezeptor.settlement.svc.client.ExpenseServiceClient;
import com.rezeptor.settlement.svc.data.BalanceData;
import com.rezeptor.settlement.svc.data.TransferData;
import com.rezeptor.settlement.svc.dto.request.SettlementCreateReq;
import com.rezeptor.settlement.svc.dto.response.BalanceDto;
import com.rezeptor.settlement.svc.dto.response.SettlementDto;
import com.rezeptor.settlement.svc.dto.response.SettlementHistoryDto;
import com.rezeptor.settlement.svc.dto.response.SettlementSuggestionDto;
import com.rezeptor.settlement.svc.dto.response.TransferDto;
import com.rezeptor.settlement.svc.entity.SettlementModel;
import com.rezeptor.settlement.svc.exception.BusinessException;
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

    @Override
    @Transactional
    public SettlementDto createSettlement(SettlementCreateReq req) {
        if (req.getFromUserId().equals(req.getToUserId())) {
            throw new BusinessException("fromUserId and toUserId must be different");
        }

        BigDecimal outstandingDebt = calculateFinalBalances(req.getGroupId()).stream()
                .filter(balance -> balance.getUserId().equals(req.getFromUserId()))
                .map(balance -> balance.getAmount().negate())
                .findFirst()
                .orElse(BigDecimal.ZERO);

        if (outstandingDebt.signum() <= 0) {
            throw new BusinessException("User " + req.getFromUserId() + " has no outstanding debt in group " + req.getGroupId());
        }
        if (req.getAmount().compareTo(outstandingDebt) > 0) {
            throw new BusinessException("Amount " + req.getAmount().toPlainString()
                    + " exceeds outstanding debt " + outstandingDebt.toPlainString() + " of user " + req.getFromUserId());
        }

        SettlementModel saved = settlementRepository.save(SettlementModel.builder()
                .groupId(req.getGroupId())
                .fromUserId(req.getFromUserId())
                .toUserId(req.getToUserId())
                .amount(req.getAmount())
                .note(req.getNote())
                .build());
        log.info("Settlement created with id: {}", saved.getId());

        return SettlementDto.builder()
                .id(saved.getId())
                .groupId(saved.getGroupId())
                .fromUserId(saved.getFromUserId())
                .toUserId(saved.getToUserId())
                .amount(saved.getAmount())
                .note(saved.getNote())
                .paidAt(saved.getPaidAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementHistoryDto> getHistory(Long groupId) {
        return settlementRepository.findHistoryByGroupId(groupId).stream()
                .map(history -> SettlementHistoryDto.builder()
                        .id(history.getId())
                        .fromUserId(history.getFromUserId())
                        .toUserId(history.getToUserId())
                        .amount(history.getAmount())
                        .note(history.getNote())
                        .paidAt(history.getPaidAt())
                        .cumulativeAmount(history.getCumulativeAmount())
                        .build())
                .toList();
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
