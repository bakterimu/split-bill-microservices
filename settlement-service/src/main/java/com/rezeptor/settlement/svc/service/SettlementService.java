package com.rezeptor.settlement.svc.service;

import com.rezeptor.settlement.svc.dto.response.SettlementSuggestionDto;

public interface SettlementService {
    SettlementSuggestionDto getSuggestion(Long groupId);
}
