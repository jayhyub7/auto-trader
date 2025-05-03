
package com.auto.trader.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.domain.User;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.repository.ApiKeyRepository;
import com.auto.trader.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final List<ExchangeService> exchangeServices;

    public List<ApiKey> getUserKeys(User user) {
        return apiKeyRepository.findAllByUser(user);
    }

    @Transactional
    public boolean saveOrUpdate(User user, Exchange exchange, String apiKey, String secretKey, String passphrase) {
        ApiKey entity = apiKeyRepository.findByUserAndExchange(user, exchange)
            .map(k -> {
                k.setApiKey(apiKey);
                k.setSecretKey(secretKey);
                k.setPassphrase(passphrase);
                return k;
            }).orElse(ApiKey.builder()
                .user(user)
                .exchange(exchange)
                .apiKey(apiKey)
                .secretKey(secretKey)
                .passphrase(passphrase)
                .build());

        boolean validated = exchangeServices.stream()
            .filter(service -> service.supports(exchange))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 거래소입니다."))
            .validate(entity);

        entity.setValidated(validated);
        apiKeyRepository.save(entity);
        return validated;
    }
    
    @Transactional
    public void deleteKey(User user, Exchange exchange) {
        apiKeyRepository.findByUserAndExchange(user, exchange)
            .ifPresent(apiKeyRepository::delete);
    }
}
