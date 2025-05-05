package com.auto.trader.position.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.domain.User;
import com.auto.trader.position.dto.PositionDto;
import com.auto.trader.position.service.PositionService;
import com.auto.trader.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService positionService;
    private final UserService userService;
    
    @GetMapping
    public List<PositionDto> getUserPositions(@AuthenticationPrincipal OAuth2User principal) {
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return positionService.findAllByUser(user);
    }
    
    @PostMapping
    public List<PositionDto> saveAll(
            @RequestBody List<PositionDto> positions,
            @AuthenticationPrincipal OAuth2User principal) {

        String email = principal.getAttribute("email");
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        return positionService.saveAll(positions, user);
    }
    
    @PostMapping("/delete")
    public void deleteAll(@RequestBody List<Long> ids) {
        positionService.deleteByIds(ids);
    }
    
    @DeleteMapping("/{id}")
    public void deleteOne(@PathVariable("id") Long id) {
        positionService.deleteById(id);
    }

}
