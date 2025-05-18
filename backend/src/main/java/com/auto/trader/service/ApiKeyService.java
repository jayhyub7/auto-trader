
package com.auto.trader.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.domain.User;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.exchange.impl.BinanceServiceImpl;
import com.auto.trader.repository.ApiKeyRepository;
import com.auto.trader.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
	@PersistenceContext
	private EntityManager entityManager;

	private final UserRepository userRepository;
	private final ApiKeyRepository apiKeyRepository;
	private final List<ExchangeService> exchangeServices;
	private final Map<String, ApiKey> keyCache = new ConcurrentHashMap<>();

	public List<ApiKey> getUserKeys(User user) {
		return apiKeyRepository.findAllByUser(user);
	}

	public ApiKey getValidatedKey(User user, Exchange exchange) {
		String cacheKey = user.getId() + "_" + exchange.name();

		// 1. 캐시에 있으면 바로 반환
		if (keyCache.containsKey(cacheKey)) {
			return keyCache.get(cacheKey);
		}

		// 2. 없으면 DB 조회 후 조건 확인
		ApiKey key = apiKeyRepository
			.findByUserAndExchange(user, exchange)
			.filter(ApiKey::isValidated)
			.orElseThrow(() -> new IllegalStateException("인증된 API 키가 없습니다."));

		keyCache.put(cacheKey, key); // ✅ 캐시에 저장
		return key;
	}

	@Transactional
	public boolean saveOrUpdate(User user, Exchange exchange, String apiKey, String secretKey, String passphrase) {
		keyCache.remove(user.getId() + "_" + exchange.name());

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		ApiKey entity = apiKeyRepository.findByUserAndExchange(user, exchange).map(k -> {
			k.setApiKey(apiKey);
			k.setSecretKey(secretKey);
			k.setPassphrase(passphrase);
			return k;
		})
			.orElse(ApiKey
				.builder()
				.user(user)
				.exchange(exchange)
				.apiKey(apiKey)
				.secretKey(secretKey)
				.passphrase(passphrase)
				.build());

		ExchangeService service = exchangeServices
			.stream()
			.filter(s -> s.supports(exchange))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("지원하지 않는 거래소입니다."));

		boolean validated = service.validate(entity);

		// ✅ Binance면 Hedge Mode 활성화
		if (exchange == Exchange.BINANCE && service instanceof BinanceServiceImpl binanceService) {
			binanceService.enableHedgeMode(entity);
		}

		entityManager.flush();
		entityManager.clear();
		entity.setValidated(validated);

		apiKeyRepository.save(entity);
		return validated;
	}

	@Transactional
	public void deleteKey(User user, Exchange exchange) {
		keyCache.remove(user.getId() + "_" + exchange.name());
		apiKeyRepository.findByUserAndExchange(user, exchange).ifPresent(apiKeyRepository::delete);
	}
}
