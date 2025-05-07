package com.auto.trader.trade;

import java.util.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.Operator;
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
        List<Position> activePositions = positionOpenService.findEnabledPositionsWithOpen();
        for (Position p : activePositions) {
            p.getConditions().size(); // 강제 초기화
        }

        for (Position position : activePositions) {
            logPositionDetails(position);
            PositionOpen positionOpen = position.getPositionOpenList().get(0);
            boolean isPass = true;

            try {
                for (var cond : position.getConditions()) {
                    Timeframe timeframe = cond.getTimeframe();
                    String key = "BTCUSDT_" + timeframe.getLabel();
                    IndicatorCache cache = IndicatorMemoryStore.get(key);

                    if (cache == null) {
                        log.warn("⚠️ 지표 캐시 없음: {}", key);
                        isPass = false;
                        break;
                    }

                    var operator = cond.getOperator();

                    switch (cond.getType()) {
                        case RSI -> {
                            var value = cond.getValue();
                            var rsiList = cache.getRsi();

                            if (!rsiList.isEmpty()) {
                                var latest = rsiList.get(rsiList.size() - 1);
                                double currentRsi = latest.getValue();
                                log.debug("🧪 RSI 검사 | 현재: {}, 기준: {}, 연산자: {}", currentRsi, value, operator);

                                if (operator == Operator.이상) {
                                    if (currentRsi < value) {
                                        log.debug("❌ RSI 실패: {} < {}", currentRsi, value);
                                        isPass = false;
                                    } else {
                                        log.debug("✅ RSI 통과");
                                    }
                                }

                                if (operator == Operator.이하) {
                                    if (currentRsi > value) {
                                        log.debug("❌ RSI 실패: {} > {}", currentRsi, value);
                                        isPass = false;
                                    } else {
                                        log.debug("✅ RSI 통과");
                                    }
                                }
                            } else {
                                log.warn("⚠️ RSI 리스트 비어 있음");
                                isPass = false;
                            }
                        }

                        case StochRSI -> {
                            var value = cond.getValue();
                            var kTarget = cond.getK();
                            var dTarget = cond.getD();
                            var stochList = cache.getStochRsi();

                            if (!stochList.isEmpty()) {
                                var latest = stochList.get(stochList.size() - 1);
                                double currentK = latest.getK();
                                double currentD = latest.getD();
                                log.debug("🧪 StochRSI 검사 | K: {}, D: {}, 기준: {}, 연산자: {}", currentK, currentD, value, operator);

                                if (operator == Operator.이상 && currentK < value) {
                                    log.debug("❌ StochRSI 실패: {} < {}", currentK, value);
                                    isPass = false;
                                } else if (operator == Operator.이하 && currentK > value) {
                                    log.debug("❌ StochRSI 실패: {} > {}", currentK, value);
                                    isPass = false;
                                } else {
                                    log.debug("✅ K값 조건 통과");
                                }

                                if (kTarget != null && dTarget != null) {
                                    if (currentK > currentD && currentK - currentD >= 0.5) {
                                        log.debug("✅ 교차 조건 통과 (%K > %D)");
                                    } else {
                                        log.debug("❌ 교차 조건 실패 (%K={}, %D={}, 차이={})", currentK, currentD, currentK - currentD);
                                        isPass = false;
                                    }
                                }
                            } else {
                                log.warn("⚠️ StochRSI 리스트 비어 있음");
                                isPass = false;
                            }
                        }

                        case VWBB -> {
                            var basis = cache.getVwbb().getBasis();
                            var upper = cache.getVwbb().getUpper();
                            var lower = cache.getVwbb().getLower();
                            double currentPrice = cache.getCurrentPrice();

                            if (!basis.isEmpty()) {
                                double upperBand = upper.get(upper.size() - 1).getValue();
                                double lowerBand = lower.get(lower.size() - 1).getValue();

                                log.debug("🧪 VWBB 검사 | 현재가: {}, 상단: {}, 하단: {}, 연산자: {}", currentPrice, upperBand, lowerBand, operator);

                                if (operator == Operator.이상 && currentPrice <= upperBand) {
                                    log.debug("❌ 상단 조건 실패 ({} <= {})", currentPrice, upperBand);
                                    isPass = false;
                                } else if (operator == Operator.이상) {
                                    log.debug("✅ 상단 조건 통과");
                                }

                                if (operator == Operator.이하 && currentPrice >= lowerBand) {
                                    log.debug("❌ 하단 조건 실패 ({} >= {})", currentPrice, lowerBand);
                                    isPass = false;
                                } else if (operator == Operator.이하) {
                                    log.debug("✅ 하단 조건 통과");
                                }
                            } else {
                                log.warn("⚠️ VWBB 기준선 없음");
                                isPass = false;
                            }
                        }
                    }

                    if (!isPass) {
                        log.debug("❌ 조건 미달성, 다음 포지션으로");
                        break;
                    }
                }

                if (isPass) {
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
 
            log.info("📋 포지션 정보 (JSON):{}", objectMapper.writeValueAsString(logMap));

        } catch (Exception e) {
            log.error("🚨 JSON 로그 변환 실패: {}", position.getId(), e);
        }
    }
}
