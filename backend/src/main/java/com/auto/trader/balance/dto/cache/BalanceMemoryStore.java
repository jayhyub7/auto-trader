package com.auto.trader.balance.dto.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.auto.trader.balance.dto.BalanceDto;

public class BalanceMemoryStore {
  private static final Map<Long, List<BalanceDto>> cache = new ConcurrentHashMap<>();

  public static List<BalanceDto> get(Long userId) {
    return cache.getOrDefault(userId, List.of());
  }

  public static void put(Long userId, List<BalanceDto> balances) {
    cache.put(userId, balances);
  }
}
