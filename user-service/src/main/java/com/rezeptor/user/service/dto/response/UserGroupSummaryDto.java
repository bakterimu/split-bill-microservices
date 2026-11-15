package com.rezeptor.user.service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rezeptor.user.service.constant.Constant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class UserGroupSummaryDto {
    private Long id;
    private String name;
    private Long memberCount;
    @JsonFormat(pattern = Constant.DATE_TIME_FORMAT)
    private LocalDateTime createdAt;
}
