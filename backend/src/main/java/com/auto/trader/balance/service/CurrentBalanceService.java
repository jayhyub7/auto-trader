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

@Service
@RequiredArgsConstructor
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

            if (key == null || !key.isValidated()) {
                result.add(new ExchangeBalanceDto(exchange.name(), false, 0.0, List.of()));
                continue;
            }

            try {
                ExchangeService service = findService(exchange);
                List<BalanceDto> balances = service.fetchBalances(key);
                double total = balances.stream().mapToDouble(BalanceDto::total).sum();
                result.add(new ExchangeBalanceDto(exchange.name(), true, total, balances));
            } catch (Exception e) {
                result.add(new ExchangeBalanceDto(exchange.name(), false, 0.0, List.of()));
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