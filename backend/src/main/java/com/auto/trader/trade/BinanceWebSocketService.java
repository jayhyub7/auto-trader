package com.auto.trader.trade;

import java.math.BigDecimal;
import java.net.URI;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import com.auto.trader.trade.indicator.IndicatorProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceWebSocketService {

	private final IndicatorProcessor indicatorProcessor;

	@SuppressWarnings("deprecation")
	@PostConstruct
	public void connect() {
		String url = "wss://stream.binance.com:9443/stream?streams=" + String
			.join("/", "btcusdt@kline_1m", "btcusdt@kline_5m", "btcusdt@kline_15m", "btcusdt@kline_1h",
					"btcusdt@kline_4h");
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

		Executors.newSingleThreadExecutor().submit(() -> {
			new StandardWebSocketClient().doHandshake(new WebSocketHandler() {
				final ObjectMapper mapper = new ObjectMapper();

				@Override
				public void afterConnectionEstablished(WebSocketSession session) {
					log.info("✅ Binance WebSocket 연결 성공");
				}

				@Override
				public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
					try {
						JsonNode json = mapper.readTree(message.getPayload().toString());
						String stream = json.get("stream").asText();
						JsonNode kline = json.get("data").get("k");

						String timeframe = stream.split("@")[1].replace("kline_", "");
						long time = kline.get("t").asLong();
						double open = new BigDecimal(kline.get("o").asText()).doubleValue();
						double high = new BigDecimal(kline.get("h").asText()).doubleValue();
						double low = new BigDecimal(kline.get("l").asText()).doubleValue();
						double close = new BigDecimal(kline.get("c").asText()).doubleValue();
						double volume = new BigDecimal(kline.get("v").asText()).doubleValue();

						indicatorProcessor.handleCandle("BTCUSDT", timeframe, time, open, high, low, close, volume);

					} catch (Exception e) {
						log.error("❌ WebSocket 메시지 처리 실패", e);
					}
				}

				@Override
				public void handleTransportError(WebSocketSession session, Throwable exception) {
					log.error("🚨 WebSocket 전송 오류", exception);
				}

				@Override
				public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
					log.warn("🔌 WebSocket 연결 종료: {}", closeStatus);
				}

				@Override
				public boolean supportsPartialMessages() {
					return false;
				}
			}, headers, URI.create(url));
		});
	}
}
