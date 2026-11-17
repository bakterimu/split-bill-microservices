package com.rezeptor.expense.svc.service.impl;

import com.rezeptor.expense.svc.constant.SplitType;
import com.rezeptor.expense.svc.data.ShareResultData;
import com.rezeptor.expense.svc.dto.request.ParticipantReq;
import com.rezeptor.expense.svc.exception.BusinessException;
import com.rezeptor.expense.svc.service.SplitService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class PercentageSplitServiceImpl implements SplitService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public SplitType type() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public List<ShareResultData> split(BigDecimal amount, List<ParticipantReq> participants) {
        if (participants.stream().map(ParticipantReq::getValue).anyMatch(Objects::isNull)) {
            throw new BusinessException("Every participant must have a percentage value");
        }

        BigDecimal totalPercent = BigDecimal.ZERO;
        for (ParticipantReq participant : participants) {
            BigDecimal value = participant.getValue();
            totalPercent = totalPercent.add(value);
        }
        if (totalPercent.compareTo(HUNDRED) != 0) {
            throw new BusinessException("Total percentage must be 100, but was " + totalPercent.stripTrailingZeros().toPlainString());
        }

        Long largestUserId = participants.stream()
                .max(Comparator.comparing(ParticipantReq::getValue)
                        .thenComparing(ParticipantReq::getUserId, Comparator.reverseOrder()))
                .map(ParticipantReq::getUserId)
                .orElseThrow();

        List<ShareResultData> rounded = participants.stream()
                .map(p -> new ShareResultData(p.getUserId(),
                        amount.multiply(p.getValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)))
                .toList();

        BigDecimal difference = amount.subtract(rounded.stream()
                .map(ShareResultData::shareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return rounded.stream()
                .map(s -> s.userId().equals(largestUserId)
                        ? new ShareResultData(s.userId(), s.shareAmount().add(difference))
                        : s)
                .toList();
    }
}
