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
import java.util.List;
import java.util.Objects;

@Service
public class ExactSplitServiceImpl implements SplitService {

    @Override
    public SplitType type() {
        return SplitType.EXACT;
    }

    @Override
    public List<ShareResultData> split(BigDecimal amount, List<ParticipantReq> participants) {
        if (participants.stream().map(ParticipantReq::getValue).anyMatch(Objects::isNull)) {
            throw new BusinessException("Every participant must have an exact amount value");
        }

        BigDecimal total = participants.stream()
                .map(ParticipantReq::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(amount) != 0) {
            throw new BusinessException("Total of exact values must equal amount " + amount.toPlainString()
                    + ", but was " + total.toPlainString());
        }

        return participants.stream()
                .map(p -> new ShareResultData(p.getUserId(), p.getValue().setScale(2, RoundingMode.UNNECESSARY)))
                .toList();
    }
}
