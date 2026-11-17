package com.rezeptor.settlement.svc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = "client.expense-service")
public class ExpenseServiceProperties {
    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;
}
