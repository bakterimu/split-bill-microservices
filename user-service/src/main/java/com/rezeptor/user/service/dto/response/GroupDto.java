package com.rezeptor.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class GroupDto {
    private Long id;
    private String name;
    private Long createdByUserId;
    private List<UserGroupDto> members;
}
