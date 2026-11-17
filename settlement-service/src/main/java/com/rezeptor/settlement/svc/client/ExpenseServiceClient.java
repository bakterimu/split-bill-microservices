package com.rezeptor.settlement.svc.client;

import com.rezeptor.settlement.svc.data.ApiResponseData;
import com.rezeptor.settlement.svc.data.BalanceData;
import com.rezeptor.settlement.svc.data.ExpenseBalanceData;
import com.rezeptor.settlement.svc.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExpenseServiceClient {
    private final RestClient expenseServiceRestClient;

    public List<BalanceData> getBalances(Long groupId) {
        ApiResponseData<List<ExpenseBalanceData>> response;
        try {
            response = expenseServiceRestClient.get()
                    .uri("/expense/balance?groupId={groupId}", groupId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            log.error("Failed to call expense-service for group {}: {}", groupId, ex.getMessage());
            throw new ServiceUnavailableException("expense-service is unavailable, please try again later");
        }

        if (response == null || response.getData() == null) {
            throw new ServiceUnavailableException("expense-service returned an empty response");
        }

        return response.getData().stream()
                .map(balance -> new BalanceData(balance.getUserId(), balance.getNetBalance()))
                .toList();
    }
}
