package com.auto.trader.trade.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import com.auto.trader.exchange.dto.OrderResult;
import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.trade.entity.TradeCondition;
import com.auto.trader.trade.entity.TradeLog;
import com.auto.trader.trade.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeLogService {

  private final TradeLogRepository tradeLogRepository;

  public TradeLog saveTradeLogWithConditions(OrderResult result, Position position,
      PositionOpen positionOpen) {
    TradeLog tradeLog = TradeLog.builder().positionTitle(position.getTitle())
        .exchange(position.getExchange().name()).symbol("BTCUSDT")
        .direction(position.getDirection().name()).quantity(positionOpen.getAmount())
        .success(result.isSuccess()).orderId(result.getOrderId()).price(result.getPrice())
        .response(result.getRawResponse()).executed(true).executedAt(LocalDateTime.now()).build();

    for (IndicatorCondition cond : position.getConditions()) {
      TradeCondition tradeCondition = TradeCondition.builder().tradeLog(tradeLog)
          .type(cond.getType()).operator(cond.getOperator()).value(cond.getValue()).k(cond.getK())
          .d(cond.getD()).timeframe(cond.getTimeframe()).conditionPhase(cond.getConditionPhase())
          .enabled(cond.getEnabled()).build();

      tradeLog.getConditions().add(tradeCondition);
    }

    return tradeLogRepository.save(tradeLog);
  }
}
