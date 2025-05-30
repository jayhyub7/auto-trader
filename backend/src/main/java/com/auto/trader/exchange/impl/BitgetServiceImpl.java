package com.auto.trader.exchange.impl;

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
import com.auto.trader.exchange.dto.OrderFeeResult;
import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.exchange.dto.SignedRequest;
import com.auto.trader.position.enums.Direction;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BitgetServiceImpl extends AbstractExchangeService implements ExchangeService {

	private static final String BASE_URL = "https://api.bitget.com";
	private static final String ACCOUNT_PATH = "/api/v2/mix/account/account";
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public boolean supports(Exchange exchange) {
		return exchange == Exchange.BITGET;
	}

	@Override
	public List<BalanceDto> fetchBalances(ApiKey key) {
		try {
			String queryString = "symbol=BTCUSDT&productType=USDT-FUTURES&marginCoin=USDT";
			SignedRequest signed = buildSignedRequest(key, ACCOUNT_PATH, queryString, HttpMethod.GET);
			String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();

			ResponseEntity<Map<String, Object>> responseEntity = getWithHeaders(url, signed.getHeaders());
			Map<String, Object> response = responseEntity.getBody();

			if (response == null || !response.containsKey("data"))
				return List.of();

			Map<String, Object> data = (Map<String, Object>) response.get("data");

			String asset = (String) data.get("marginCoin");
			double available = Double.parseDouble((String) data.getOrDefault("available", "0"));
			double locked = Double.parseDouble((String) data.getOrDefault("locked", "0"));

			return List.of(toBalanceDto(asset, available, locked, Exchange.BYBIT));
		} catch (Exception e) {
			log.error("❌ Bitget 잔고 조회 실패", e);
			return List.of();
		}
	}

	@Override
	public boolean validate(ApiKey key) {
		try {
			String queryString = "symbol=BTCUSDT&productType=USDT-FUTURES&marginCoin=USDT";
			SignedRequest signed = buildSignedRequest(key, ACCOUNT_PATH, queryString, HttpMethod.GET);
			String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();

			getWithHeaders(url, signed.getHeaders());
			return true;
		} catch (Exception e) {
			log.error("❌ Bitget 인증 실패", e);
			return false;
		}
	}

	@Override
	public SignedRequest buildSignedRequest(ApiKey apiKey, String path, String payload, HttpMethod method) {
		try {
			String serverTimeUrl = BASE_URL + "/api/v2/public/time";
			Map<String, Object> timeResponse = getWithHeaders(serverTimeUrl, new HttpHeaders()).getBody();
			if (timeResponse == null || !timeResponse.containsKey("data")) {
				throw new IllegalStateException("Bitget 서버 시간 응답이 잘못되었습니다.");
			}

			String timestamp = String.valueOf(((Map<String, Object>) timeResponse.get("data")).get("serverTime"));
			String methodStr = method.name();
			String data = payload != null ? payload : "";

			String preHash = (method == HttpMethod.GET) ? timestamp + methodStr + path + "?" + data
					: timestamp + methodStr + path + data;

			String sign = hmacSha256WithBase64Encoding(preHash, apiKey.getSecretKey().trim());

			HttpHeaders headers = new HttpHeaders();
			headers.set("ACCESS-KEY", apiKey.getApiKey());
			headers.set("ACCESS-TIMESTAMP", timestamp);
			headers.set("ACCESS-SIGN", sign);
			headers.set("ACCESS-PASSPHRASE", apiKey.getPassphrase());
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));

			return new SignedRequest(headers, data); // data = queryString (GET) or bodyJson (POST)
		} catch (Exception e) {
			throw new RuntimeException("❌ Bitget Header 생성 실패", e);
		}
	}

	// 시장가 주문
	@Override
	public OrderResult placeMarketOrder(ApiKey key, String symbol, double quantity, Direction direction,
			Double stopLossPrice, Double takeProfitPrice) {
		long startTime = System.nanoTime();

		try {
			OrderResult result = sendMarketOrder(key, symbol, quantity, direction);
			if (!result.isSuccess())
				return result;

			boolean slSuccess = true;
			boolean tpSuccess = true;

			// SL 등록 시도
			if (stopLossPrice != null) {
				try {
					placePlanOrder(key, symbol, quantity, direction, stopLossPrice, "loss_plan");
				} catch (Exception e) {
					log.error("❌ Bitget SL 등록 실패", e);
					slSuccess = false;
				}
			}

			// TP 등록 시도
			if (takeProfitPrice != null) {
				try {
					placePlanOrder(key, symbol, quantity, direction, takeProfitPrice, "profit_plan");
				} catch (Exception e) {
					log.error("❌ Bitget TP 등록 실패", e);
					tpSuccess = false;
				}
			}

			// 결과 작성
			double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;

			OrderResult finalResult = new OrderResult(true, result.getOrderId(), symbol, quantity, result.getPrice(),
					result.getRawResponse());

			finalResult.setTpSlSuccess(slSuccess && tpSuccess);
			finalResult.setExecutionTimeSeconds(elapsed);

			return finalResult;

		} catch (Exception e) {
			log.error("❌ Bitget 주문 실패", e);
			return new OrderResult(false, null, symbol, 0, 0, e.getMessage());
		}
	}

	// 시장가 주문 실행
	private OrderResult sendMarketOrder(ApiKey key, String symbol, double quantity, Direction direction) {
		String path = "/api/v2/mix/order/place-order";
		String url = BASE_URL + path;

		String side = (direction == Direction.LONG) ? "buy" : "sell";

		Map<String, Object> bodyMap = Map
			.of("symbol", symbol, "marginCoin", "USDT", "side", side, "orderType", "market", "size",
					String.valueOf(quantity), "productType", "USDT-FUTURES");

		String bodyJson = toJsonString(bodyMap);
		SignedRequest signed = buildSignedRequest(key, path, bodyJson, HttpMethod.POST);

		HttpEntity<String> entity = new HttpEntity<>(bodyJson, signed.getHeaders());
		ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

		Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) response.getBody().get("data"));

		return new OrderResult(true, String.valueOf(data.get("orderId")), symbol,
				Double.parseDouble(data.get("size").toString()), 0, data.toString());
	}

	// TP/SL
	private void placePlanOrder(ApiKey key, String symbol, double quantity, Direction direction, double triggerPrice,
			String planType) {
		String path = "/api/v2/mix/order/place-plan-order";
		String url = BASE_URL + path;

		String side = (direction == Direction.LONG) ? "sell" : "buy";

		Map<String, Object> bodyMap = Map
			.of("symbol", symbol, "marginCoin", "USDT", "side", side, "orderType", "market", "triggerPrice",
					String.valueOf(triggerPrice), "planType", planType, // "loss_plan"
																		// or
																		// "profit_plan"
					"size", String.valueOf(quantity), "productType", "USDT-FUTURES", "triggerType", "mark_price");

		String bodyJson = toJsonString(bodyMap);
		SignedRequest signed = buildSignedRequest(key, path, bodyJson, HttpMethod.POST);

		HttpEntity<String> entity = new HttpEntity<>(bodyJson, signed.getHeaders());
		restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
	}

	@Override
	public boolean placeStopLossOrder(ApiKey key, String symbol, double quantity, double stopLossPrice,
			Direction direction) {
		try {
			placePlanOrder(key, symbol, quantity, direction, stopLossPrice, "loss_plan");
			return true;
		} catch (Exception e) {
			log.error("❌ Bitget SL 등록 실패", e);
			return false;
		}
	}

	@Override
	public boolean placeTakeProfitOrder(ApiKey key, String symbol, double quantity, double takeProfitPrice,
			Direction direction) {
		try {
			placePlanOrder(key, symbol, quantity, direction, takeProfitPrice, "profit_plan");
			return true;
		} catch (Exception e) {
			log.error("❌ Bitget TP 등록 실패", e);
			return false;
		}
	}

	@Override
	public void setLeverage(ApiKey apiKey, String symbol, int leverage) {
		try {
			String path = "/api/mix/v1/account/setLeverage";

			Map<String, Object> bodyMap = new HashMap<>();
			bodyMap.put("symbol", symbol);
			bodyMap.put("marginCoin", "USDT");
			bodyMap.put("leverage", String.valueOf(leverage));
			bodyMap.put("holdSide", "both");

			String bodyJson = objectMapper.writeValueAsString(bodyMap);
			SignedRequest signed = buildSignedRequest(apiKey, path, bodyJson, HttpMethod.POST);

			HttpHeaders headers = signed.getHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
			restTemplate.postForEntity(BASE_URL + path + "?" + signed.getQueryString(), entity, String.class);
		} catch (Exception e) {
			log.error("❌ Bitget 레버리지 설정 실패: symbol={}, leverage={}", symbol, leverage, e);
		}
	}

	@Override
	public OrderFeeResult fetchOrderFee(ApiKey key, String symbol, String orderId) {
		try {
			String path = "/api/mix/v1/order/fills";
			String url = "https://api.bitget.com" + path + "?orderId=" + orderId;

			SignedRequest signed = buildSignedRequest(key, path, "orderId=" + orderId, HttpMethod.GET);
			HttpHeaders headers = signed.getHeaders();
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
			Map<String, Object> result = response.getBody();
			List<Map<String, Object>> fills = (List<Map<String, Object>>) result.get("data");

			double totalFee = 0.0;
			double totalValue = 0.0;
			String feeCoin = "USDT";

			for (Map<String, Object> fill : fills) {
				double price = Double.parseDouble(fill.get("price").toString());
				double size = Double.parseDouble(fill.get("size").toString());
				double fee = Double.parseDouble(fill.get("fee").toString());
				String feeAsset = fill.get("feeCoin").toString();

				totalFee += fee;
				totalValue += price * size;
				feeCoin = feeAsset;
			}

			double feeRate = (totalValue > 0) ? totalFee / totalValue : 0.0;

			return OrderFeeResult.builder().feeAmount(totalFee).feeCurrency(feeCoin).feeRate(feeRate).build();

		} catch (Exception e) {
			log.error("❌ Bitget 수수료 조회 실패: {}", e.getMessage());
			throw new RuntimeException("Bitget 수수료 조회 실패", e);
		}
	}

}
