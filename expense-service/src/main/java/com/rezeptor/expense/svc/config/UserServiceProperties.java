package com.rezeptor.expense.svc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = "client.user-service")
public class UserServiceProperties {
    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;
}
