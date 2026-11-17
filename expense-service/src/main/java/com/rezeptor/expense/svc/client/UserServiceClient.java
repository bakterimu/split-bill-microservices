package com.rezeptor.expense.svc.client;

import com.rezeptor.expense.svc.data.ApiResponseData;
import com.rezeptor.expense.svc.data.UserData;
import com.rezeptor.expense.svc.exception.ServiceUnavailableException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {
    private final RestClient userServiceRestClient;

    public List<Long> getMemberIds(Long groupId) {
        ApiResponseData<List<UserData>> response;
        try {
            response = userServiceRestClient.get()
                    .uri("/group/{groupId}/members", groupId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (HttpClientErrorException.NotFound ex) {
            throw new EntityNotFoundException("Group not found with id: " + groupId);
        } catch (RestClientException ex) {
            log.error("Failed to call user-service for group {}: {}", groupId, ex.getMessage());
            throw new ServiceUnavailableException("user-service is unavailable, please try again later");
        }

        if (response == null || response.getData() == null) {
            throw new ServiceUnavailableException("user-service returned an empty response");
        }

        return response.getData().stream()
                .map(UserData::getId)
                .toList();
    }
}
