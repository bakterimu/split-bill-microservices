package com.rezeptor.expense.svc.service.impl;

import com.rezeptor.expense.svc.data.ShareResultData;
import com.rezeptor.expense.svc.dto.request.ExpenseCreateReq;
import com.rezeptor.expense.svc.dto.request.ParticipantReq;
import com.rezeptor.expense.svc.dto.response.ExpenseDto;
import com.rezeptor.expense.svc.dto.response.ExpenseShareDto;
import com.rezeptor.expense.svc.entity.ExpenseModel;
import com.rezeptor.expense.svc.exception.BusinessException;
import com.rezeptor.expense.svc.repository.ExpenseRepository;
import com.rezeptor.expense.svc.service.ExpenseService;
import com.rezeptor.expense.svc.service.SplitServiceResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final SplitServiceResolver splitServiceResolver;

    @Override
    @Transactional
    public ExpenseDto createExpense(ExpenseCreateReq req) {
        validateNoDuplicateParticipant(req.getParticipants());

        List<ShareResultData> shares = splitServiceResolver.resolve(req.getSplitType())
                .split(req.getAmount(), req.getParticipants());

        ExpenseModel expense = ExpenseModel.builder()
                .groupId(req.getGroupId())
                .paidBy(req.getPaidBy())
                .description(req.getDescription().trim())
                .category(req.getCategory())
                .amount(req.getAmount())
                .splitType(req.getSplitType())
                .expenseDate(req.getExpenseDate())
                .build();
        shares.forEach(share -> expense.addShare(share.getUserId(), share.getShareAmount()));

        ExpenseModel saved = expenseRepository.save(expense);
        log.info("Expense created with id: {}", saved.getId());
        return toDto(saved);
    }

    private void validateNoDuplicateParticipant(List<ParticipantReq> participants) {
        List<Long> duplicateUserIds = participants.stream()
                .collect(Collectors.groupingBy(ParticipantReq::getUserId, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!duplicateUserIds.isEmpty()) {
            throw new BusinessException("Duplicate participants: " + duplicateUserIds);
        }
    }

    private ExpenseDto toDto(ExpenseModel expense) {
        return ExpenseDto.builder()
                .id(expense.getId())
                .groupId(expense.getGroupId())
                .paidBy(expense.getPaidBy())
                .description(expense.getDescription())
                .category(expense.getCategory())
                .amount(expense.getAmount())
                .splitType(expense.getSplitType())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .shares(expense.getShares().stream()
                        .map(share -> ExpenseShareDto.builder()
                                .userId(share.getUserId())
                                .shareAmount(share.getShareAmount())
                                .build())
                        .toList())
                .build();
    }
}
