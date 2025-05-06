package com.auto.trader.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.Timeframe;
import com.auto.trader.position.service.PositionOpenService;
import com.auto.trader.position.service.PositionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeScheduler {

    private final PositionService positionService;
    private final PositionOpenService positionOpenService;
    private final ObjectMapper objectMapper;  
    
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void checkAndExecuteTrades() {
        //log.info("🔁 포지션 조건 확인 중...");
    	
        List<Position> activePositions = positionOpenService.findEnabledPositionsWithOpen();
        for (Position p : activePositions) {
            p.getConditions().size(); // 강제 초기화
        }

        //log.info("✅ 활성화된 포지션 수: {}", activePositions.size());

        for (Position position : activePositions) {
        	logPositionDetails(position);
        	// 현재 1개만 등록이 가능하게 되어 있다. 추후 변경될 수도 있음.
        	PositionOpen positionOpen = position.getPositionOpenList().get(0);
            try {
                for (var cond : position.getConditions()) {
                	
                	Timeframe timeframe = cond.getTimeframe();  // 예: "1m"
                	//System.out.println(timeframe.getLabel());
                    String key = "BTCUSDT_" + timeframe.getLabel();
                    IndicatorCache cache = IndicatorMemoryStore.get(key);                    
                    
                    if (cache == null) {
                        log.warn("⚠️ 지표 캐시 없음: {}", key);
                        continue;
                    }

                    switch (cond.getType()) {
                        case RSI -> {
                            var rsiList = cache.getRsi();
                            if (!rsiList.isEmpty()) {
                                var latest = rsiList.get(rsiList.size() - 1);
                                
                                //log.info("📊 [RSI][{}] 조건 ID {} → 현재값: {}", timeframe, cond.getId(), latest.getValue());
                            }
                        }
                        case StochRSI -> {
                            var stochList = cache.getStochRsi();
                            if (!stochList.isEmpty()) {
                                var latest = stochList.get(stochList.size() - 1);
                                //log.info("📊 [StochRSI][{}] 조건 ID {} → K: {}, D: {}", timeframe, cond.getId(), latest.getK(), latest.getD());
                            }
                        }
                        case VWBB -> {
                            var basis = cache.getVwbb().getBasis();
                            var upper = cache.getVwbb().getUpper();
                            var lower = cache.getVwbb().getLower();
                            if (!basis.isEmpty() && !upper.isEmpty() && !lower.isEmpty()) {
                            	/*
                                log.info("📊 [VWBB][{}] 조건 ID {} → Basis: {}, Upper: {}, Lower: {}",
                                        timeframe, cond.getId(),
                                        basis.get(basis.size() - 1).getValue(),
                                        upper.get(upper.size() - 1).getValue(),
                                        lower.get(lower.size() - 1).getValue());
                                        */
                            }
                        }
                    }
                }

                // 현재는 실제 조건 판단 대신 지표 로그만 출력 중
                boolean shouldEnterTrade = false;

                if (shouldEnterTrade) {
                    log.info("✅ 진입 조건 만족 → 매매 실행 예정: {}", position.getTitle());
                }

            } catch (Exception e) {
                log.error("🚨 포지션 처리 중 오류: " + position.getId(), e);
            }
        }

    }

    private void logPositionDetails(Position position) {
        try {
            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("id", position.getId());
            logMap.put("title", position.getTitle());
            logMap.put("exchange", position.getExchange());
            logMap.put("enabled", position.isEnabled());

            if (position.getUser() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", position.getUser().getId());
                userMap.put("email", position.getUser().getEmail());
                logMap.put("user", userMap);
            }

            List<Map<String, Object>> conditionList = new ArrayList<>();
            for (var cond : position.getConditions()) {
                Map<String, Object> condMap = new HashMap<>();
                condMap.put("id", cond.getId());
                condMap.put("type", cond.getType());
                condMap.put("operator", cond.getOperator());
                condMap.put("value", cond.getValue());
                condMap.put("k", cond.getK());
                condMap.put("d", cond.getD());
                condMap.put("timeframe", cond.getTimeframe());
                condMap.put("direction", cond.getDirection());
                condMap.put("conditionPhase", cond.getConditionPhase());
                conditionList.add(condMap);
            }
            logMap.put("conditions", conditionList);

            List<Map<String, Object>> openList = new ArrayList<>();
            if (position.getPositionOpenList() != null) {
                for (var open : position.getPositionOpenList()) {
                    Map<String, Object> openMap = new HashMap<>();
                    openMap.put("id", open.getId());
                    openMap.put("amount", open.getAmount());
                    openMap.put("amountType", open.getAmountType());
                    openMap.put("stopLoss", open.getStopLoss());
                    openMap.put("takeProfit", open.getTakeProfit());
                    openMap.put("status", open.getStatus());
                    openList.add(openMap);
                }
            }
            logMap.put("positionOpenList", openList);

            log.info("📋 포지션 정보 (JSON):\n{}", objectMapper.writeValueAsString(logMap));

        } catch (Exception e) {
            log.error("🚨 JSON 로그 변환 실패: {}", position.getId(), e);
        }
    }
}
