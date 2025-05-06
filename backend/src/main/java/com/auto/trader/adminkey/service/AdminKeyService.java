package com.auto.trader.adminkey.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.adminkey.entity.AdminKey;
import com.auto.trader.adminkey.repository.AdminKeyRepository;
import com.auto.trader.domain.ApiKey; // ✅ 재활용
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.ExchangeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKeyService {

    private final AdminKeyRepository adminKeyRepository;
    private final List<ExchangeService> exchangeServices;

    public Optional<AdminKey> getBinanceKey() {
        return adminKeyRepository.findByExchange(Exchange.BINANCE);
    }
    @Transactional
    public AdminKey saveBinanceKey(String apiKey, String secretKey) {
        Exchange exchange = Exchange.BINANCE;

        AdminKey adminKey = AdminKey.builder()
            .exchange(exchange)
            .apiKey(apiKey)
            .secretKey(secretKey)
            .validated(false)
            .build();

        // ✅ ApiKey 객체로 임시 래핑하여 validate 재활용
        ApiKey fake = ApiKey.builder()
            .exchange(exchange)
            .apiKey(apiKey)
            .secretKey(secretKey)
            .build();

        boolean validated = exchangeServices.stream()
            .filter(service -> service.supports(exchange))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 거래소입니다."))
            .validate(fake);

        adminKey.setValidated(validated);

        // 기존 키 삭제 후 새로 저장
        adminKeyRepository.deleteByExchange(exchange);
        return adminKeyRepository.save(adminKey);
    }
    @Transactional
    public void deleteBinanceKey() {
        adminKeyRepository.deleteByExchange(Exchange.BINANCE);
    }
}
