package com.auto.trader.exchange.impl;

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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BinanceServiceImpl extends AbstractExchangeService implements ExchangeService {

	private static final String BASE_URL = "https://api.binance.com";
	private static final String ACCOUNT_PATH = "/api/v3/account";

	@Override
	public boolean supports(Exchange exchange) {
		return exchange == Exchange.BINANCE;
	}

	@Override
	public List<BalanceDto> fetchBalances(ApiKey key) {
		try {
			SignedRequest signed = buildSignedRequest(key, null, null, HttpMethod.GET);
			String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();
			Map<String, Object> response = getWithHeaders(url, signed.getHeaders()).getBody();
			List<Map<String, Object>> rawBalances = (List<Map<String, Object>>) response.get("balances");
			return parseBalances(rawBalances);
		} catch (Exception e) {
			return List.of();
		}
	}

	public void enableHedgeMode(ApiKey key) {
		try {
			String path = "/fapi/v1/positionSide/dual";
			String url = "https://fapi.binance.com" + path;

			long timestamp = fetchBinanceServerTime();
			String body = "dualSidePosition=true&timestamp=" + timestamp + "&recvWindow=10000";
			String signature = hmacSha256(body, key.getSecretKey());
			String fullQuery = body + "&signature=" + signature;

			HttpHeaders headers = new HttpHeaders();
			headers.set("X-MBX-APIKEY", key.getApiKey());
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			HttpEntity<String> entity = new HttpEntity<>("", headers);

			ResponseEntity<Map> response = restTemplate
				.exchange(url + "?" + fullQuery, HttpMethod.POST, entity, Map.class);

			Map<String, Object> bodyMap = response.getBody();
			if (bodyMap != null && "-4059".equals(String.valueOf(bodyMap.get("code")))) {
				log.info("☑ Binance Hedge Mode 이미 설정되어 있음 (code -4059)");
			} else {
				log.info("✅ Binance Hedge Mode 활성화 응답: {}", bodyMap);
			}

		} catch (Exception e) {
			log.error("❌ Binance Hedge Mode 설정 실패", e);
		}
	}

	@Override
	public boolean validate(ApiKey key) {
		try {
			SignedRequest signed = buildSignedRequest(key, null, null, HttpMethod.GET);
			String url = BASE_URL + ACCOUNT_PATH + "?" + signed.getQueryString();
			getWithHeaders(url, signed.getHeaders());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected List<BalanceDto> parseBalances(List<Map<String, Object>> rawBalances) {
		return rawBalances.stream().map(b -> {
			String asset = (String) b.get("asset");
			double available = Double.parseDouble(b.get("free").toString());
			double locked = Double.parseDouble(b.get("locked").toString());
			return toBalanceDto(asset, available, locked, Exchange.BINANCE);
		}).filter(dto -> dto.getTotal() > 0).toList();
	}

	@Override
	public SignedRequest buildSignedRequest(ApiKey apiKey, String pathOrQuery, String payload, HttpMethod method) {
		long timestamp = fetchBinanceServerTime();
		String recvWindow = "10000";

		String queryString;
		if (method == HttpMethod.GET) {
			queryString = (payload == null || payload.isEmpty())
					? "recvWindow=" + recvWindow + "&timestamp=" + timestamp
					: payload + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;
		} else {
			queryString = "recvWindow=" + recvWindow + "&timestamp=" + timestamp;
		}

		String signature = hmacSha256(queryString, apiKey.getSecretKey());
		String signedQuery = queryString + "&signature=" + signature;

		HttpHeaders headers = new HttpHeaders();
		headers.set("X-MBX-APIKEY", apiKey.getApiKey());
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		return new SignedRequest(headers, signedQuery);
	}

	private long fetchBinanceServerTime() {
		String url = BASE_URL + "/api/v3/time";
		Map<String, Object> response = getWithHeaders(url, new HttpHeaders()).getBody();
		if (response == null || !response.containsKey("serverTime")) {
			throw new IllegalStateException("Binance 서버 시간 응답 오류");
		}
		return Long.parseLong(response.get("serverTime").toString());
	}

	@Override
	public OrderResult placeMarketOrder(ApiKey key, String symbol, double quantity, Direction direction,
			Double stopLossPrice, Double takeProfitPrice) {
		try {
			OrderResult result = sendMarketOrder(key, symbol, quantity, direction);
			if (!result.isSuccess())
				return result;

			if (stopLossPrice != null) {
				placeConditionalOrder(key, symbol, quantity, direction, stopLossPrice, "STOP_MARKET");
			}

			if (takeProfitPrice != null) {
				placeConditionalOrder(key, symbol, quantity, direction, takeProfitPrice, "TAKE_PROFIT_MARKET");
			}

			return result;

		} catch (Exception e) {
			log.error("❌ Binance 시장가 주문 실패", e);
			return new OrderResult(false, null, symbol, 0, 0, e.getMessage());
		}
	}

	private OrderResult sendMarketOrder(ApiKey key, String symbol, double quantity, Direction direction) {
		String path = "/api/v3/order";
		String url = BASE_URL + path;

		String side = (direction == Direction.LONG) ? "BUY" : "SELL";
		String positionSide = (direction == Direction.LONG) ? "LONG" : "SHORT";
		long timestamp = fetchBinanceServerTime();
		String recvWindow = "10000";

		String body = "symbol=" + symbol + "&side=" + side + "&positionSide=" + positionSide + "&type=MARKET"
				+ "&quantity=" + quantity + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;

		String signature = hmacSha256(body, key.getSecretKey());
		String fullQuery = body + "&signature=" + signature;

		HttpHeaders headers = new HttpHeaders();
		headers.set("X-MBX-APIKEY", key.getApiKey());
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		try {
			HttpEntity<String> entity = new HttpEntity<>("", headers);
			ResponseEntity<Map<String, Object>> responseEntity = restTemplate
				.exchange(url + "?" + fullQuery, HttpMethod.POST, entity,
						(Class<Map<String, Object>>) (Class<?>) Map.class);

			Map<String, Object> res = responseEntity.getBody();
			return new OrderResult(true, String.valueOf(res.get("orderId")), symbol,
					Double.parseDouble(res.get("executedQty").toString()),
					Double.parseDouble(res.getOrDefault("price", "0").toString()), res.toString());
		} catch (Exception e) {
			log.error("❌ Binance 시장가 주문 실패", e);
			return new OrderResult(false, null, symbol, 0, 0, e.getMessage());
		}
	}

	private void placeConditionalOrder(ApiKey key, String symbol, double quantity, Direction direction,
			double triggerPrice, String type) {
		String path = "/api/v3/order";
		String url = BASE_URL + path;

		String side = (direction == Direction.LONG) ? "SELL" : "BUY";
		String positionSide = (direction == Direction.LONG) ? "LONG" : "SHORT";
		long timestamp = fetchBinanceServerTime();
		String recvWindow = "10000";

		String body = "symbol=" + symbol + "&side=" + side + "&positionSide=" + positionSide + "&type=" + type
				+ "&quantity=" + quantity + "&stopPrice=" + triggerPrice + "&timeInForce=GTC" + "&reduceOnly=true"
				+ "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;

		String signature = hmacSha256(body, key.getSecretKey());
		String fullQuery = body + "&signature=" + signature;

		HttpHeaders headers = new HttpHeaders();
		headers.set("X-MBX-APIKEY", key.getApiKey());
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		HttpEntity<String> entity = new HttpEntity<>("", headers);
		restTemplate.exchange(url + "?" + fullQuery, HttpMethod.POST, entity, Map.class);
	}

	@Override
	public boolean placeStopLossOrder(ApiKey key, String symbol, double quantity, double stopLossPrice,
			Direction direction) {
		try {
			placeConditionalOrder(key, symbol, quantity, direction, stopLossPrice, "STOP_MARKET");
			return true;
		} catch (Exception e) {
			log.error("❌ Binance Stop Loss 주문 실패", e);
			return false;
		}
	}

	@Override
	public boolean placeTakeProfitOrder(ApiKey key, String symbol, double quantity, double takeProfitPrice,
			Direction direction) {
		try {
			placeConditionalOrder(key, symbol, quantity, direction, takeProfitPrice, "TAKE_PROFIT_MARKET");
			return true;
		} catch (Exception e) {
			log.error("❌ Binance Take Profit 주문 실패", e);
			return false;
		}
	}

	@Override
	public void setLeverage(ApiKey apiKey, String symbol, int leverage) {
		try {
			String path = "/fapi/v1/leverage";
			String payload = "symbol=" + symbol + "&leverage=" + leverage;

			SignedRequest signed = buildSignedRequest(apiKey, path, payload, HttpMethod.POST);

			HttpEntity<String> entity = new HttpEntity<>(null, signed.getHeaders());
			ResponseEntity<String> result = restTemplate
				.postForEntity(BASE_URL + path + "?" + signed.getQueryString(), entity, String.class);
			log.info("setLeverage result : {}", result);
		} catch (Exception e) {
			log.error("❌ Binance 레버리지 설정 실패: symbol={}, leverage={}", symbol, leverage, e);
		}
	}

	@Override
	public OrderFeeResult fetchOrderFee(ApiKey key, String symbol, String orderId) {
		try {
			String path = "/fapi/v1/userTrades";
			String payload = "symbol=" + symbol + "&orderId=" + orderId;
			SignedRequest signed = buildSignedRequest(key, path, payload, HttpMethod.GET);
			String url = "https://fapi.binance.com" + path + "?" + signed.getQueryString();

			ResponseEntity<List> response = restTemplate
				.exchange(url, HttpMethod.GET, new HttpEntity<>(signed.getHeaders()), List.class);

			List<Map<String, Object>> trades = response.getBody();
			double totalFee = 0.0;
			String currency = null;
			double totalValue = 0.0;

			for (Map<String, Object> trade : trades) {
				double price = Double.parseDouble(trade.get("price").toString());
				double qty = Double.parseDouble(trade.get("qty").toString());
				double fee = Double.parseDouble(trade.get("commission").toString());
				String commissionAsset = trade.get("commissionAsset").toString();

				totalFee += fee;
				totalValue += price * qty;
				currency = commissionAsset;
			}

			double feeRate = (totalValue > 0) ? totalFee / totalValue : 0.0;

			return OrderFeeResult.builder().feeAmount(totalFee).feeCurrency(currency).feeRate(feeRate).build();

		} catch (Exception e) {
			log.error("❌ Binance 수수료 조회 실패: {}", e.getMessage());
			throw new RuntimeException("Binance 수수료 조회 실패", e);
		}
	}

}
