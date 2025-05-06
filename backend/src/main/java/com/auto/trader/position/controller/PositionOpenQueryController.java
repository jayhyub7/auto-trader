package com.auto.trader.position.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.domain.User;
import com.auto.trader.position.dto.PositionDto;
import com.auto.trader.position.dto.PositionOpenDto;
import com.auto.trader.position.service.PositionOpenService;
import com.auto.trader.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/position-opens")
@RequiredArgsConstructor
public class PositionOpenQueryController {

    private final PositionOpenService positionOpenService;
    private final UserService userService;

    @GetMapping
    public List<PositionDto> getMyOpenPositions(@AuthenticationPrincipal OAuth2User principal) {
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        return positionOpenService.getOpenPositionsForUser(user);
    }
}