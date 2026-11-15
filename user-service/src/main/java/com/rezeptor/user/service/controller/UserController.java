package com.rezeptor.user.service.controller;

import com.rezeptor.user.service.dto.request.UserCreateReq;
import com.rezeptor.user.service.dto.response.Response;
import com.rezeptor.user.service.dto.response.UserCreatedDto;
import com.rezeptor.user.service.dto.response.UserGroupSummaryDto;
import com.rezeptor.user.service.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateReq userCreateReq) {
        UserCreatedDto user = userService.createUser(userCreateReq.getName(), userCreateReq.getEmail());
        return Response.created(user).toEntity();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        return Response.ok(userService.getUserById(userId)).toEntity();
    }

    @GetMapping("/{userId}/group")
    public ResponseEntity<?> getListGroupUser(@PathVariable Long userId) {
        List<UserGroupSummaryDto> listGroup = userService.getListGroup(userId);
        return Response.ok(listGroup).toEntity();
    }
}
