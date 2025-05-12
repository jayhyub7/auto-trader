package com.auto.trader.trade.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PositionLogUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void log(Position position) {
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
      for (IndicatorCondition cond : position.getConditions()) {
        Map<String, Object> condMap = new HashMap<>();
        condMap.put("id", cond.getId());
        condMap.put("type", cond.getType());
        condMap.put("operator", cond.getOperator());
        condMap.put("value", cond.getValue());
        condMap.put("k", cond.getK());
        condMap.put("d", cond.getD());
        condMap.put("timeframe", cond.getTimeframe());
        condMap.put("conditionPhase", cond.getConditionPhase());
        conditionList.add(condMap);
      }
      logMap.put("conditions", conditionList);

      List<Map<String, Object>> openList = new ArrayList<>();
      if (position.getPositionOpenList() != null) {
        for (PositionOpen open : position.getPositionOpenList()) {
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

      log.debug("📋 포지션 정보 (JSON): {}", objectMapper.writeValueAsString(logMap));

    } catch (Exception e) {
      log.error("🚨 포지션 로그 변환 실패: {}", position.getId(), e);
    }
  }
}
