package com.rezeptor.user.service.entity.projection;

import java.time.LocalDateTime;

public interface UserGroupSummaryProjection {
    Long getId();
    String getName();
    Long getMemberCount();
    LocalDateTime getCreatedAt();
}
