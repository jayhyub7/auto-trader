package com.auto.trader.trade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.math.BigDecimal;
import java.net.URI;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceWebSocketService {

    private final CandleAggregator candleAggregator;

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
                        BigDecimal price = new BigDecimal(json.get("p").asText());  // 거래 가격
                        long timestamp = json.get("T").asLong();                    // 거래 시각 (ms)

                        candleAggregator.handleTick("BTCUSDT", price.doubleValue(), timestamp);

                    } catch (Exception e) {
                        log.error("❌ WebSocket 메시지 처리 실패", e);
                    }
                }

                @Override public void handleTransportError(WebSocketSession session, Throwable exception) {
                    log.error("🚨 WebSocket 전송 오류", exception);
                }

                @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                    log.warn("🔌 WebSocket 연결 종료: {}", closeStatus);
                }

                @Override public boolean supportsPartialMessages() {
                    return false;
                }
            }, headers, URI.create(url));
        });
    }
}
