// src/main/java/com/auto/trader/controller/ApiKeyController.java
package com.auto.trader.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.domain.User;
import com.auto.trader.service.ApiKeyService;
import com.auto.trader.service.UserService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getKeys(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            System.out.println("⚠️ principal is null");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String email = (String) principal.getAttributes().get("email");
        
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        List<ApiKeyResponse> list = apiKeyService.getUserKeys(user).stream()
                .map(ApiKeyResponse::new)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<?> saveKey(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody ApiKeyRequest request
    ) {
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean validated = apiKeyService.saveOrUpdate(
            user,
            request.exchange,
            request.apiKey != null ? request.apiKey.trim() : null,
            request.secretKey != null ? request.secretKey.trim() : null,
            request.passphrase != null ? request.passphrase.trim() : null
        );

        return ResponseEntity.ok(Map.of("validated", validated));
    }


    @DeleteMapping("/{exchange}")
    public ResponseEntity<?> deleteKey(@AuthenticationPrincipal OAuth2User principal, @PathVariable("exchange") Exchange exchange) {
        String email = (String) principal.getAttributes().get("email");
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        apiKeyService.deleteKey(user, exchange);
        return ResponseEntity.ok().build();
    }

    // DTOs
    @Data
    public static class ApiKeyRequest {
        public Exchange exchange;
        public String apiKey;
        public String secretKey;
        public String passphrase;
    }

    @Data
    @AllArgsConstructor
    public static class ApiKeyResponse {
        private Exchange exchange;
        private String apiKey;
        private String secretKey;
        private String passphrase;
        private boolean validated;

        public ApiKeyResponse(ApiKey entity) {
            this.exchange = entity.getExchange();
            this.apiKey = entity.getApiKey();
            this.secretKey = entity.getSecretKey();
            this.passphrase = entity.getPassphrase();
            this.validated = entity.isValidated(); // ✅ 추가된 필드
        }
    }

}