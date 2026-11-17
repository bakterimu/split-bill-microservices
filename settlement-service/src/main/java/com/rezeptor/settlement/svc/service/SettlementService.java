package com.rezeptor.settlement.svc.service;

import com.rezeptor.settlement.svc.dto.request.SettlementCreateReq;
import com.rezeptor.settlement.svc.dto.response.SettlementDto;
import com.rezeptor.settlement.svc.dto.response.SettlementHistoryDto;
import com.rezeptor.settlement.svc.dto.response.SettlementSuggestionDto;

import java.util.List;

public interface SettlementService {
    SettlementSuggestionDto getSuggestion(Long groupId);
    SettlementDto createSettlement(SettlementCreateReq req);
    List<SettlementHistoryDto> getHistory(Long groupId);
}
