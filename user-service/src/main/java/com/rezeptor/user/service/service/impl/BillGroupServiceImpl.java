package com.rezeptor.user.service.service.impl;

import com.rezeptor.user.service.dto.request.GroupCreateReq;
import com.rezeptor.user.service.dto.response.GroupCreatedDto;
import com.rezeptor.user.service.dto.response.GroupDto;
import com.rezeptor.user.service.dto.response.UserGroupDto;
import com.rezeptor.user.service.entity.BillGroupModel;
import com.rezeptor.user.service.entity.UserGroupModel;
import com.rezeptor.user.service.entity.UserModel;
import com.rezeptor.user.service.repository.BillGroupRepository;
import com.rezeptor.user.service.repository.UserGroupRepository;
import com.rezeptor.user.service.repository.UserRepository;
import com.rezeptor.user.service.service.BillGroupService;
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
public class BillGroupServiceImpl implements BillGroupService {
    private BillGroupRepository groupRepository;
    private UserRepository userRepository;
    private UserGroupRepository userGroupRepository;

    @Override
    @Transactional
    public GroupCreatedDto createGroup(GroupCreateReq req) {
        Optional<UserModel> userById = userRepository.findById(req.getCreatedByUserId());
        if (userById.isEmpty()) {
            throw new EntityNotFoundException("User not found");
        }

        BillGroupModel group = BillGroupModel.builder()
                .name(req.getName())
                .createdBy(userById.get())
                .build();

        group.addMember(userById.get());
        group = groupRepository.save(group);
        log.info("Group created with id: {}", group.getId());

        return GroupCreatedDto.builder()
                .id(group.getId())
                .name(group.getName())
                .createdByUserId(userById.get().getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDto getGroupById(Long id) {
        BillGroupModel group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        List<UserGroupModel> memberInGroupById = userGroupRepository.findMemberInGroupById(id);
        List<UserGroupDto> listMember = memberInGroupById.stream()
                .map(mem -> UserGroupDto
                        .builder()
                        .userId(mem.getUser().getId())
                        .name(mem.getUser().getName())
                        .joinedAt(mem.getJoinedAt())
                        .build())
                .toList();

        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .createdByUserId(group.getCreatedBy().getId())
                .members(listMember)
                .build();
    }

    @Override
    @Transactional
    public void addMember(Long groupId, Long userId) {
        Optional<UserModel> userOpt = userRepository.findById(userId);
        Optional<BillGroupModel> groupOpt = groupRepository.findById(groupId);

        if(userOpt.isEmpty()) {
            throw new EntityNotFoundException("User not found");
        }

        if (groupOpt.isEmpty()) {
            throw new EntityNotFoundException("Group not found");
        }

        groupOpt.get().addMember(userOpt.get());
        groupRepository.save(groupOpt.get());
        log.info("Member added with id: {}", userOpt.get().getId());
    }
}
