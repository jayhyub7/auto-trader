package com.auto.trader.adminkey.controller;

import com.auto.trader.adminkey.entity.AdminKey;
import com.auto.trader.adminkey.service.AdminKeyService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin-key")
@RequiredArgsConstructor
public class AdminKeyController {

    private final AdminKeyService adminKeyService;

    // ✅ 조회
    @GetMapping
    public ResponseEntity<AdminKey> getAdminKey() {
        Optional<AdminKey> key = adminKeyService.getBinanceKey();
        return key.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ✅ 저장 (업데이트 포함)
    @PostMapping
    public ResponseEntity<AdminKey> saveKey(@RequestBody AdminKey request) {
        AdminKey saved = adminKeyService.saveBinanceKey(request.getApiKey(), request.getSecretKey());
        return ResponseEntity.ok(saved);
    }

    // ✅ 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteKey() {
        adminKeyService.deleteBinanceKey();
        return ResponseEntity.noContent().build();
    }
}
