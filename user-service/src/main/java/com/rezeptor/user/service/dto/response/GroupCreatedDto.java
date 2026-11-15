package com.rezeptor.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Builder
@Getter
public class GroupCreatedDto {
    private Long id;
    private String name;
    private Long createdByUserId;
}
