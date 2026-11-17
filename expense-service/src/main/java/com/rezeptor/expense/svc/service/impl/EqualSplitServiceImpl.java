package com.rezeptor.expense.svc.service.impl;

import com.rezeptor.expense.svc.constant.SplitType;
import com.rezeptor.expense.svc.data.ShareResultData;
import com.rezeptor.expense.svc.dto.request.ParticipantReq;
import com.rezeptor.expense.svc.service.SplitService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class EqualSplitServiceImpl implements SplitService {
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    @Override
    public SplitType type() {
        return SplitType.EQUAL;
    }

    @Override
    public List<ShareResultData> split(BigDecimal amount, List<ParticipantReq> participants) {
        List<Long> userIds = participants.stream()
                .map(ParticipantReq::getUserId)
                .sorted(Comparator.naturalOrder())
                .toList();

        BigDecimal count = BigDecimal.valueOf(userIds.size());
        BigDecimal base = amount.divide(count, 2, RoundingMode.DOWN);
        int remainingCents = amount.subtract(base.multiply(count)).divide(ONE_CENT, 0, RoundingMode.UNNECESSARY).intValue();

        return IntStream.range(0, userIds.size())
                .mapToObj(i -> new ShareResultData(userIds.get(i), i < remainingCents ? base.add(ONE_CENT) : base))
                .toList();
    }
}
