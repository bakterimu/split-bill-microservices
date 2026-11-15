package com.rezeptor.user.service.service.impl;

import com.rezeptor.user.service.dto.response.UserCreatedDto;
import com.rezeptor.user.service.dto.response.UserDto;
import com.rezeptor.user.service.dto.response.UserGroupSummaryDto;
import com.rezeptor.user.service.entity.UserModel;
import com.rezeptor.user.service.entity.projection.UserGroupSummaryProjection;
import com.rezeptor.user.service.repository.BillGroupRepository;
import com.rezeptor.user.service.repository.UserRepository;
import com.rezeptor.user.service.service.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private UserRepository userRepository;
    private BillGroupRepository groupRepository;

    @Override
    @Transactional
    public UserCreatedDto createUser(String name, String email) {
        Optional<UserModel> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            throw new EntityExistsException("User with email " + email + " already exists.");
        }

        UserModel userModel = UserModel.builder()
                .name(name)
                .email(email)
                .build();

        UserModel userSaved = userRepository.save(userModel);

        log.info("User saved with id {}", userSaved.getId());
        return UserCreatedDto.builder()
                .id(userSaved.getId())
                .name(userSaved.getName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        UserModel userModel = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        return UserDto.builder()
                .id(userModel.getId())
                .name(userModel.getName())
                .email(userModel.getEmail())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGroupSummaryDto> getListGroup(Long userId) {
        List<UserGroupSummaryProjection> userGroupSummary = groupRepository.getUserGroupSummaryProjectionById(userId);
        List<UserGroupSummaryDto> summaryDto = userGroupSummary.stream()
                .map(
                g -> UserGroupSummaryDto
                        .builder()
                        .id(g.getId())
                        .memberCount(g.getMemberCount())
                        .name(g.getName())
                        .createdAt(g.getCreatedAt())
                        .build())
                .toList();
        return summaryDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByGroup(Long groupId) {
        List<UserModel> byGroupId = userRepository.findByGroupId(groupId);
        if (byGroupId.isEmpty()) {
            throw new EntityNotFoundException("User with id " + groupId + " not found.");
        }
        List<UserDto> userList = byGroupId.stream()
                .map(
                        u -> UserDto.builder()
                                .id(u.getId())
                                .email(u.getEmail())
                                .name(u.getName())
                                .build())
                .toList();
        return userList;
    }
}
