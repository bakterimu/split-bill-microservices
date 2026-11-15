package com.rezeptor.user.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateReq {
    @NotBlank(message = "name must not be empy")
    private String name;

    @NotNull(message = "create user must not be null")
    @Positive(message = "not zero or negative")
    private Long createdByUserId;
}
