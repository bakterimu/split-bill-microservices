package com.rezeptor.expense.svc.service;

import com.rezeptor.expense.svc.constant.SplitType;
import com.rezeptor.expense.svc.data.ShareResultData;
import com.rezeptor.expense.svc.dto.request.ParticipantReq;

import java.math.BigDecimal;
import java.util.List;

public interface SplitService {
    SplitType type();

    List<ShareResultData> split(BigDecimal amount, List<ParticipantReq> participants);
}
