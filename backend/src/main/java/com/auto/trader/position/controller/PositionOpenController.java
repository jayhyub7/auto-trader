package com.auto.trader.position.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.domain.User;
import com.auto.trader.position.dto.PositionOpenRequestDto;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.PositionOpenStatus;
import com.auto.trader.position.service.PositionOpenService;
import com.auto.trader.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/position-open")
@RequiredArgsConstructor
public class PositionOpenController {

    private final PositionOpenService positionOpenService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<PositionOpenRequestDto> save(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody PositionOpenRequestDto dto
    ) {
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        PositionOpenRequestDto saved = positionOpenService.save(dto, user);
        return ResponseEntity.ok(saved); // ✅ 생성된 ID 포함 응답
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable("id") Long id,
            @RequestBody PositionOpenRequestDto dto
    ) {
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        dto.setId(id);
        positionOpenService.update(dto, user);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable("id") Long id
    ) {
    	System.out.println("delete");
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        PositionOpen open = positionOpenService.findById(id)
                .orElseThrow(() -> new RuntimeException("포지션 오픈 정보 없음"));

        if (!open.getPosition().getUser().getId().equals(user.getId())) {
            throw new SecurityException("삭제 권한 없음");
        }

        if (open.getStatus() == PositionOpenStatus.RUNNING || open.getStatus() == PositionOpenStatus.SIMULATING) {
            throw new IllegalStateException("실행 또는 시뮬레이션 상태에서는 삭제할 수 없습니다.");
        }

        positionOpenService.deleteById(id, user);
        return ResponseEntity.noContent().build();
    }

}
