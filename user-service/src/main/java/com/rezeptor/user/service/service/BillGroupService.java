package com.rezeptor.user.service.service;

import com.rezeptor.user.service.dto.request.GroupCreateReq;
import com.rezeptor.user.service.dto.response.GroupCreatedDto;
import com.rezeptor.user.service.dto.response.GroupDto;

public interface BillGroupService {
    GroupCreatedDto createGroup(GroupCreateReq req);
    GroupDto getGroupById(Long id);

    void addMember(Long groupId, Long userId);
}
