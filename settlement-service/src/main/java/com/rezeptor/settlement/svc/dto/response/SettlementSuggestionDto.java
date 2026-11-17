package com.rezeptor.settlement.svc.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class SettlementSuggestionDto {
    private Long groupId;
    private List<BalanceDto> finalBalances;
    private List<TransferDto> transfers;
    private int transferCount;
}
