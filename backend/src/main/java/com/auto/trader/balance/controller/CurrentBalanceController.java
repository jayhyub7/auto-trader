
package com.auto.trader.balance.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.balance.dto.ExchangeBalanceDto;
import com.auto.trader.balance.service.CurrentBalanceService;
import com.auto.trader.domain.User;
import com.auto.trader.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/current-balance")
public class CurrentBalanceController {

    private final CurrentBalanceService currentBalanceService;
    private final UserService userService;

    @GetMapping
    public List<ExchangeBalanceDto> getAllCurrentBalances(@AuthenticationPrincipal OAuth2User principal) {
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return currentBalanceService.getBalances(user);
    }
}
