package com.rezeptor.user.service.controller;

import com.rezeptor.user.service.dto.request.GroupCreateReq;
import com.rezeptor.user.service.dto.response.*;
import com.rezeptor.user.service.service.BillGroupService;
import com.rezeptor.user.service.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group")
@AllArgsConstructor
public class GroupController {
    private BillGroupService groupService;
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody @Valid GroupCreateReq groupDto) {
        GroupCreatedDto group = groupService.createGroup(groupDto);
        return Response.created(group).toEntity();
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId) {
        GroupDto groupById = groupService.getGroupById(groupId);
        return Response.ok(groupById).toEntity();
    }

    @PostMapping("/{groupId}/user/{userId}")
    public ResponseEntity<?> addUserToGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.addMember(groupId, userId);
        return Response.ok(null).toEntity();
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long groupId) {
        List<UserDto> listGroup = userService.getUsersByGroup(groupId);
        return Response.ok(listGroup).toEntity();
    }
}
