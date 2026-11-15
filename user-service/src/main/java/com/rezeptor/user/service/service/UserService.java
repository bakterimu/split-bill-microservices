package com.rezeptor.user.service.service;

import com.rezeptor.user.service.dto.response.UserCreatedDto;
import com.rezeptor.user.service.dto.response.UserDto;
import com.rezeptor.user.service.dto.response.UserGroupSummaryDto;

import java.util.List;

public interface UserService {
    UserCreatedDto createUser(String name, String email);
    UserDto getUserById(Long userId);
    List<UserGroupSummaryDto> getListGroup(Long userId);
    List<UserDto> getUsersByGroup(Long groupId);
}
