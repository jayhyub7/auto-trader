package com.auto.trader.controller;

import com.auto.trader.domain.User;
import com.auto.trader.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 모든 사용자 조회
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAllUsers();
    }

    // 로그인한 사용자 정보 조회
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return Map.of(
            "name", user.getName(),
            "nickName", Optional.ofNullable(user.getNickName()).orElse(""),
            "email", user.getEmail()
        );
    }
    
    // 닉네임 변경
    @PutMapping("/me/nickname")
    public User updateNickName(@AuthenticationPrincipal OAuth2User principal, @RequestBody Map<String, String> request) {
        String newNickName = request.get("nickName");
        return userService.updateNickName(principal.getAttribute("email"), newNickName);
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public void deleteUser(@AuthenticationPrincipal OAuth2User principal) {
        userService.deleteByEmail(principal.getAttribute("email"));
    }
}
