package com.auto.trader.balance.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.balance.dto.ExchangeBalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.domain.User;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.repository.ApiKeyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentBalanceService {

    private final ApiKeyRepository apiKeyRepository;
    private final List<ExchangeService> exchangeServices;

    public List<ExchangeBalanceDto> getBalances(User user) {
        List<ApiKey> keys = apiKeyRepository.findAllByUser(user);
        List<ExchangeBalanceDto> result = new ArrayList<>();

        for (Exchange exchange : Exchange.values()) {
            ApiKey key = keys.stream()
                    .filter(k -> k.getExchange() == exchange)
                    .findFirst()
                    .orElse(null);

            // ❌ 키가 없거나 인증 안된 경우는 아예 제외
            if (key == null || !key.isValidated()) {
                continue;
            }

            try {
                ExchangeService service = findService(exchange);
                List<BalanceDto> balances = service.fetchBalances(key);
                List<BalanceDto> usdtOnly = balances.stream()
                        .filter(b -> "USDT".equalsIgnoreCase(b.getAsset()))
                        .toList();
                double total = usdtOnly.stream()
                        .mapToDouble(BalanceDto::getTotal)
                        .sum();
                result.add(new ExchangeBalanceDto(exchange.name(), true, total, usdtOnly));
            } catch (Exception e) {
                // 에러 난 경우도 제외
                log.error("잔고 조회 중 오류 발생 ({}): {}", exchange.name(), e.getMessage());
            }
        }

        return result;
    }

    private ExchangeService findService(Exchange exchange) {
        return exchangeServices.stream()
                .filter(s -> s.supports(exchange))
                .findFirst()
                .orElseThrow();
    }
} 
