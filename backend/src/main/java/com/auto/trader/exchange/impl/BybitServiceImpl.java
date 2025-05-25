package com.auto.trader.exchange.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.exchange.AbstractExchangeService;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.exchange.dto.SignedRequest;
import com.auto.trader.position.enums.Direction;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BybitServiceImpl extends AbstractExchangeService implements ExchangeService {

	private static final String BASE_URL = "https://api.bybit.com";
	private static final String ACCOUNT_PATH = "/v5/account/wallet-balance";
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public boolean supports(Exchange exchange) {
		return exchange == Exchange.BYBIT;
	}

	@Override
	public List<BalanceDto> fetchBalances(ApiKey key) {
		try {
			String queryString = "accountType=UNIFIED";
			SignedRequest signed = buildSignedRequest(key, ACCOUNT_PATH, queryString, HttpMethod.GET);
			String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();

			Map<String, Object> response = getWithHeaders(url, signed.getHeaders()).getBody();
			List<Map<String, Object>> rawBalances = (List<Map<String, Object>>) ((Map<String, Object>) response
				.get("result")).get("list");
			return parseBalances(rawBalances);
		} catch (Exception e) {
			return List.of();
		}
	}

	@Override
	public boolean validate(ApiKey key) {
		try {
			String queryString = "accountType=UNIFIED";
			SignedRequest signed = buildSignedRequest(key, ACCOUNT_PATH, queryString, HttpMethod.GET);
			String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();

			getWithHeaders(url, signed.getHeaders());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected List<BalanceDto> parseBalances(List<Map<String, Object>> rawBalances) {
		List<BalanceDto> result = new ArrayList<>();

		for (Map<String, Object> balanceMap : rawBalances) {
			Object coinObj = balanceMap.get("coin");
			if (!(coinObj instanceof List<?>))
				continue;

			List<?> coins = (List<?>) coinObj;
			for (Object c : coins) {
				if (!(c instanceof Map))
					continue;

				Map<String, Object> coinMap = (Map<String, Object>) c;
				String asset = (String) coinMap.get("coin");

				double walletBalance = parseDouble(coinMap.get("walletBalance"));
				double totalPositionIM = parseDouble(coinMap.get("totalPositionIM"));
				double available = walletBalance - totalPositionIM;
				double locked = 0.0;
				double usdValue = parseDouble(coinMap.get("usdValue"));

				BalanceDto dto = new BalanceDto(asset, available, locked, walletBalance, usdValue, Exchange.BYBIT);
				if (dto.getTotal() > 0) {
					result.add(dto);
				}
			}
		}

		return result;
	}

	private double parseDouble(Object value) {
		try {
			return value != null ? Double.parseDouble(value.toString()) : 0.0;
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	@Override
	public SignedRequest buildSignedRequest(ApiKey apiKey, String path, String payload, HttpMethod method) {
		try {
			String timeUrl = BASE_URL + "/v5/market/time";
			Map<String, Object> timeResponse = getWithHeaders(timeUrl, new HttpHeaders()).getBody();

			if (timeResponse == null || !timeResponse.containsKey("time")) {
				throw new IllegalStateException("Bybit 서버 시간 응답이 잘못되었습니다.");
			}

			long timestamp = Long.parseLong(timeResponse.get("time").toString());
			String recvWindow = "10000";

			String data = payload != null ? payload : "";

			// ✅ GET이면 queryString, POST면 body로 처리
			String preHash = (method == HttpMethod.GET) ? timestamp + apiKey.getApiKey() + recvWindow + data
					: timestamp + apiKey.getApiKey() + recvWindow + data;

			String signature = hmacSha256(preHash, apiKey.getSecretKey());

			HttpHeaders headers = new HttpHeaders();
			headers.set("X-BAPI-API-KEY", apiKey.getApiKey());
			headers.set("X-BAPI-SIGN", signature);
			headers.set("X-BAPI-TIMESTAMP", String.valueOf(timestamp));
			headers.set("X-BAPI-RECV-WINDOW", recvWindow);
			headers.set("X-BAPI-SIGN-TYPE", "2");
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_JSON);

			return new SignedRequest(headers, data); // GET이면 queryString, POST면 bodyJson
		} catch (Exception e) {
			throw new RuntimeException("❌ Bybit Header 생성 실패", e);
		}
	}

	@Override
	public OrderResult placeMarketOrder(ApiKey key, String symbol, double quantity, Direction direction,
			Double stopLossPrice, Double takeProfitPrice) {

		long startTime = System.nanoTime();

		try {
			String path = "/v5/order/create";
			String url = BASE_URL + path;

			String side = direction == Direction.LONG ? "Buy" : "Sell";

			Map<String, Object> bodyMap = new HashMap<>();
			bodyMap.put("category", "linear");
			bodyMap.put("symbol", symbol);
			bodyMap.put("side", side);
			bodyMap.put("orderType", "Market");
			bodyMap.put("qty", String.valueOf(quantity));
			bodyMap.put("timeInForce", "GTC");

			if (stopLossPrice != null) {
				bodyMap.put("stopLoss", String.valueOf(stopLossPrice));
				bodyMap.put("slTriggerBy", "LastPrice");
			}
			if (takeProfitPrice != null) {
				bodyMap.put("takeProfit", String.valueOf(takeProfitPrice));
				bodyMap.put("tpTriggerBy", "LastPrice");
			}

			String bodyJson = toJsonString(bodyMap);
			SignedRequest signed = buildSignedRequest(key, path, bodyJson, HttpMethod.POST);

			HttpEntity<String> entity = new HttpEntity<>(bodyJson, signed.getHeaders());
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

			Map<String, Object> data = (Map<String, Object>) ((Map) response.getBody().get("result"));

			double avgPrice = Double.parseDouble(data.get("cumExecValue").toString())
					/ Double.parseDouble(data.get("cumExecQty").toString());

			double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;

			OrderResult result = new OrderResult(true, String.valueOf(data.get("orderId")), symbol,
					Double.parseDouble(data.get("cumExecQty").toString()), avgPrice, response.getBody().toString());

			result.setTpSlSuccess(true);
			result.setExecutionTimeSeconds(elapsed);

			return result;

		} catch (Exception e) {
			log.error("❌ Bybit 주문 실패", e);
			return new OrderResult(false, null, symbol, 0, 0, e.getMessage());
		}
	}

	@Override
	public boolean placeStopLossOrder(ApiKey key, String symbol, double quantity, double stopLossPrice,
			Direction direction) {
		try {
			placePlanOrder(key, symbol, quantity, direction, stopLossPrice, "loss");
			return true;
		} catch (Exception e) {
			log.error("❌ Bybit StopLoss 주문 실패", e);
			return false;
		}
	}

	@Override
	public boolean placeTakeProfitOrder(ApiKey key, String symbol, double quantity, double takeProfitPrice,
			Direction direction) {
		try {
			placePlanOrder(key, symbol, quantity, direction, takeProfitPrice, "profit");
			return true;
		} catch (Exception e) {
			log.error("❌ Bybit TakeProfit 주문 실패", e);
			return false;
		}
	}

	private void placePlanOrder(ApiKey key, String symbol, double quantity, Direction direction, double triggerPrice,
			String tpSlType) {
		String path = "/v5/order/create";
		String url = BASE_URL + path;

		String side = direction == Direction.LONG ? (tpSlType.equals("loss") ? "Sell" : "Sell")
				: (tpSlType.equals("loss") ? "Buy" : "Buy"); // 롱일 때 손절은 매도, 숏일 땐 매수

		Map<String, Object> bodyMap = new HashMap<>();
		bodyMap.put("category", "linear");
		bodyMap.put("symbol", symbol);
		bodyMap.put("side", side);
		bodyMap.put("orderType", "Market");
		bodyMap.put("triggerPrice", String.valueOf(triggerPrice));
		bodyMap.put("triggerDirection", direction == Direction.LONG ? 1 : 2); // 예시: 1은 위로 돌파, 2는 아래로 돌파
		bodyMap.put("timeInForce", "GTC");
		bodyMap.put("qty", String.valueOf(quantity));
		bodyMap.put("positionIdx", 0);
		bodyMap.put("reduceOnly", true);
		bodyMap.put("isLeverage", true);

		String bodyJson = toJsonString(bodyMap);
		SignedRequest signed = buildSignedRequest(key, path, bodyJson, HttpMethod.POST);
		HttpEntity<String> entity = new HttpEntity<>(bodyJson, signed.getHeaders());
		restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
	}

	@Override
	public void setLeverage(ApiKey apiKey, String symbol, int leverage) {
		try {
			String path = "/v5/position/set-leverage";

			Map<String, Object> payloadMap = new HashMap<>();
			payloadMap.put("symbol", symbol);
			payloadMap.put("buyLeverage", String.valueOf(leverage));
			payloadMap.put("sellLeverage", String.valueOf(leverage));

			String payload = objectMapper.writeValueAsString(payloadMap);

			SignedRequest signed = buildSignedRequest(apiKey, path, payload, HttpMethod.POST);

			HttpHeaders headers = signed.getHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> entity = new HttpEntity<>(payload, headers);
			restTemplate.postForEntity(BASE_URL + path + "?" + signed.getQueryString(), entity, String.class);
		} catch (Exception e) {
			log.error("❌ Bybit 레버리지 설정 실패: symbol={}, leverage={}", symbol, leverage, e);
		}
	}

}
