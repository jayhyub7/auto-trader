package com.auto.trader.trade;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.balance.dto.cache.BalanceMemoryStore;
import com.auto.trader.domain.ApiKey;
import com.auto.trader.exchange.ExchangeRouter;
import com.auto.trader.exchange.ExchangeService;
import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.AmountType;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.PositionOpenStatus;
import com.auto.trader.position.enums.Timeframe;
import com.auto.trader.position.repository.PositionOpenRepository;
import com.auto.trader.position.service.PositionOpenService;
import com.auto.trader.position.service.PositionService;
import com.auto.trader.service.ApiKeyService;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.repository.TradeConditionRepository;
import com.auto.trader.trade.repository.TradeLogRepository;
import com.auto.trader.trade.service.ExecutedOrderService;
import com.auto.trader.trade.service.TradeLogService;
import com.auto.trader.trade.util.PositionLogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntryTradeScheduler {

  private final PositionService positionService;
  private final PositionOpenService positionOpenService;
  private final ObjectMapper objectMapper;
  private final PositionOpenRepository positionOpenRepository;
  private final TradeLogService tradeLogService;
  private final TradeLogRepository tradeLogRepository;
  private final TradeConditionRepository tradeConditionRepository;
  private final ExecutedOrderService executedOrderService;
  private final ExchangeRouter exchangeRouter;
  private final ApiKeyService apiKeyService;


  // @Scheduled(initialDelay = 7000, fixedDelay = 1500)
  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void checkEntryPosition() {
    List<Position> activePositions = positionOpenService.findEntryPosition();
    for (Position p : activePositions) {
      p.getConditions().size(); // 강제 초기화
    }

    for (Position position : activePositions) {
      PositionLogUtil.log(position);

      PositionOpen positionOpen = position.getPositionOpenList().get(0);
      if (positionOpen.isExecuted()) {
        log.info("⏸️ 중복 방지: 이미 실행된 포지션 - {}", position.getTitle());
        continue;
      }
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
          log.info("현재 Condition : {}", cond.getOperator());
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

            case STOCH_RSI -> {
              var value = cond.getValue();
              var kTarget = cond.getK();
              var dTarget = cond.getD();
              var stochList = cache.getStochRsi();

              if (!stochList.isEmpty()) {
                var latest = stochList.get(stochList.size() - 1);
                double currentK = latest.getK();
                double currentD = latest.getD();
                log.debug("🧪 StochRSI 검사 | K: {}, D: {}, 기준: {}, 연산자: {}", currentK, currentD,
                    value, operator);

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
                    log.debug("❌ 교차 조건 실패 (%K={}, %D={}, 차이={})", currentK, currentD,
                        currentK - currentD);
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

                log.info("🧪 VWBB 검사 | 상단: {}, 하단: {}, 현재가: {}, 연산자: {}", upperBand, lowerBand,
                    currentPrice, operator);
                System.out.println(cache.getCandles().size());
                if (operator == Operator.상단_돌파) {
                  if (currentPrice > upperBand) {
                    log.info("✅ 상단 돌파 조건 통과 ({} > {})", currentPrice, upperBand);
                  } else {
                    log.info("❌ 상단 돌파 조건 실패 ({} <= {})", currentPrice, upperBand);
                    isPass = false;
                  }
                }

                if (operator == Operator.하단_돌파) {
                  if (currentPrice < lowerBand) {
                    log.info("✅ 하단 돌파 조건 통과 ({} < {})", currentPrice, lowerBand);
                  } else {
                    log.info("❌ 하단 돌파 조건 실패 ({} >= {})", currentPrice, lowerBand);
                    isPass = false;
                  }
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

          ApiKey apiKey = apiKeyService.getValidatedKey(position.getUser(), position.getExchange());

          // 1. stopLoss 유효성 확인
          if (!positionOpen.isValidStopLoss()) {
            throw new IllegalStateException(
                "❌ stopLoss 누락 또는 값 이상 (Position ID: " + position.getId() + ")");
          }

          // 2. 주문 직전 기준가 저장
          double observedPrice = IndicatorMemoryStore.get("BTCUSDT_1m").getCurrentPrice();

          // ✅ 3. 주문 수량 계산
          double quantity;
          if (positionOpen.getAmountType() == AmountType.PERCENT) {
            List<BalanceDto> balances = BalanceMemoryStore.get(position.getUser().getId());

            double available = balances.stream()
                .filter(b -> b.getAsset().equalsIgnoreCase("USDT")
                    && b.getExchange() == position.getExchange())
                .mapToDouble(BalanceDto::getAvailable).findFirst().orElse(0.0);

            quantity = available * (positionOpen.getAmount() / 100.0);
            log.info("📊 금액 비율 기반 수량 계산: available={} → quantity={}", available, quantity);
          } else {
            quantity = positionOpen.getAmount();
          }

          // 4. 시장가 주문 실행
          ExchangeService exchangeService = exchangeRouter.getService(position.getExchange());
          OrderResult result = null;
          if (positionOpen.getStatus().equals(PositionOpenStatus.RUNNING)) {
            result = exchangeService.placeMarketOrder(apiKey, "BTCUSDT", quantity,
                position.getDirection(), positionOpen.getStopLoss(), positionOpen.getTakeProfit());
          } else {
            result = exchangeService.createSimulatedOrder("BTCUSDT", quantity, observedPrice);
          }


          if (!result.isSuccess()) {
            throw new IllegalStateException("❌ 시장가 주문 실패: " + result.getRawResponse());
          }

          // 5. 체결가 기준 TP/SL 가격 계산
          double entryPrice = result.getPrice();
          Direction direction = position.getDirection();

          Double takeProfitPrice = positionOpen.isValidTakeProfit()
              ? calcTakeProfitPrice(entryPrice, positionOpen.getTakeProfit(), direction)
              : null;

          Double stopLossPrice =
              calcStopLossPrice(entryPrice, positionOpen.getStopLoss(), direction);

          // 6. TP/SL 주문 등록
          if (positionOpen.getStatus().equals(PositionOpenStatus.RUNNING)) {
            try {
              if (stopLossPrice != null) {
                exchangeService.placeStopLossOrder(apiKey, "BTCUSDT", result.getExecutedQty(),
                    stopLossPrice, direction);
              }
              if (takeProfitPrice != null) {
                exchangeService.placeTakeProfitOrder(apiKey, "BTCUSDT", result.getExecutedQty(),
                    takeProfitPrice, direction);
              }
            } catch (Exception e) {
              log.error("🚨 TP/SL 등록 실패: 포지션 {}", position.getId(), e);
              // TODO: ExecutedOrder에 tpSlRegistered = false 등 기록 필요
            }
          }

          // 7. 체결 저장 및 상태 갱신
          executedOrderService.saveExecutedOrderWithIndicators(result, positionOpen,
              position.getExchange().name(), "BTCUSDT", observedPrice);

          PositionLogUtil.log(position);
          tradeLogService.saveTradeLogWithConditions(result, position, positionOpen);
          positionOpen.setCurrentOrderId(result.getOrderId());
          positionOpen.setExecuted(true);
          positionOpen.setExecutedAt(LocalDateTime.now());
          positionOpenRepository.save(positionOpen);
        }


      } catch (Exception e) {
        log.error("🚨 포지션 처리 중 오류: " + position.getId(), e);
      }
    }
  }

  private double calcStopLossPrice(double entryPrice, double percent, Direction direction) {
    return direction == Direction.LONG ? entryPrice * (1 - percent) : entryPrice * (1 + percent);
  }

  private double calcTakeProfitPrice(double entryPrice, double percent, Direction direction) {
    return direction == Direction.LONG ? entryPrice * (1 + percent) : entryPrice * (1 - percent);
  }

}
