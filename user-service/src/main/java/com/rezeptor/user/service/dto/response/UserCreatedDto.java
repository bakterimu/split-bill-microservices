package com.rezeptor.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class UserCreatedDto {
    private Long id;
    private String name;
}
