package com.rezeptor.user.service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddMemberReq {
    @NotNull(message = "userId not null")
    @Positive(message = "userId greater than zero")
    private Long userId;
    @NotNull(message = "groupId not null")
    @Positive(message = "groupId greater than zero")
    private Long groupId;
}
