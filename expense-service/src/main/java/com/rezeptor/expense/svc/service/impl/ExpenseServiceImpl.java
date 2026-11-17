package com.rezeptor.expense.svc.service.impl;

import com.rezeptor.expense.svc.client.UserServiceClient;
import com.rezeptor.expense.svc.constant.Category;
import com.rezeptor.expense.svc.data.ShareResultData;
import com.rezeptor.expense.svc.dto.request.ExpenseCreateReq;
import com.rezeptor.expense.svc.dto.request.ParticipantReq;
import com.rezeptor.expense.svc.dto.response.BalanceDto;
import com.rezeptor.expense.svc.dto.response.ExpenseDto;
import com.rezeptor.expense.svc.dto.response.ExpenseShareDto;
import com.rezeptor.expense.svc.dto.response.ExpenseSummaryDto;
import com.rezeptor.expense.svc.dto.response.MonthlyReportDto;
import com.rezeptor.expense.svc.dto.response.PageDto;
import com.rezeptor.expense.svc.entity.ExpenseModel;
import com.rezeptor.expense.svc.entity.projection.ExpenseSummaryProjection;
import com.rezeptor.expense.svc.exception.BusinessException;
import com.rezeptor.expense.svc.repository.ExpenseRepository;
import com.rezeptor.expense.svc.service.ExpenseService;
import com.rezeptor.expense.svc.service.SplitServiceResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final SplitServiceResolver splitServiceResolver;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public ExpenseDto createExpense(ExpenseCreateReq req) {
        validateNoDuplicateParticipant(req.getParticipants());
        validateGroupMembers(req);

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

    @Override
    @Transactional(readOnly = true)
    public ExpenseDto getExpenseById(Long expenseId) {
        ExpenseModel expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + expenseId));
        return toDto(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<ExpenseSummaryDto> getExpensesByGroup(Long groupId, int page, int size) {
        Page<ExpenseSummaryProjection> result = expenseRepository.findSummariesByGroupId(groupId, PageRequest.of(page, size));

        List<ExpenseSummaryDto> content = result.getContent().stream()
                .map(e -> ExpenseSummaryDto.builder()
                        .id(e.getId())
                        .description(e.getDescription())
                        .category(Category.valueOf(e.getCategory()))
                        .amount(e.getAmount())
                        .paidBy(e.getPaidBy())
                        .expenseDate(e.getExpenseDate())
                        .participantCount(e.getParticipantCount())
                        .build())
                .toList();

        return PageDto.<ExpenseSummaryDto>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceDto> getBalancesByGroup(Long groupId) {
        return expenseRepository.findBalancesByGroupId(groupId).stream()
                .map(b -> BalanceDto.builder()
                        .userId(b.getUserId())
                        .totalPaid(b.getTotalPaid())
                        .totalOwed(b.getTotalOwed())
                        .netBalance(b.getNetBalance())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyReportDto> getMonthlyReport(Long groupId, int year, BigDecimal minTotal) {
        return expenseRepository.findMonthlyReport(groupId, year, minTotal).stream()
                .map(r -> MonthlyReportDto.builder()
                        .month(r.getMonth())
                        .category(Category.valueOf(r.getCategory()))
                        .transactionCount(r.getTransactionCount())
                        .totalAmount(r.getTotalAmount())
                        .averageAmount(r.getAverageAmount())
                        .build())
                .toList();
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

    private void validateGroupMembers(ExpenseCreateReq req) {
        Set<Long> memberIds = new HashSet<>(userServiceClient.getMemberIds(req.getGroupId()));

        List<Long> nonMemberIds = Stream.concat(
                        Stream.of(req.getPaidBy()),
                        req.getParticipants().stream().map(ParticipantReq::getUserId))
                .distinct()
                .filter(userId -> !memberIds.contains(userId))
                .sorted()
                .toList();

        if (!nonMemberIds.isEmpty()) {
            throw new BusinessException("Users " + nonMemberIds + " are not members of group " + req.getGroupId());
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
