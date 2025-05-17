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

	private volatile double lastPrice = 0;
	private volatile long lastTimestamp = 0;

	@PostConstruct
	public void connect() {
		String url = "wss://stream.binance.com:9443/ws/btcusdt@trade";
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
						BigDecimal price = new BigDecimal(json.get("p").asText());
						long timestamp = json.get("T").asLong();

						lastPrice = price.doubleValue(); // 👉 최신 가격 저장
						lastTimestamp = timestamp; // 👉 최신 시간 저장

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

	// ✅ 1초마다 마지막 가격을 기반으로 처리
	@jakarta.annotation.PostConstruct
	@org.springframework.scheduling.annotation.Scheduled(fixedRate = 1000)
	public void processLatestPrice() {
		if (lastPrice != 0 && lastTimestamp != 0) {
			indicatorProcessor.handleTick("BTCUSDT", lastPrice, lastTimestamp);
		}
	}

}
