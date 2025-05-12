package com.auto.trader.trade.indicator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class IndicatorMemoryStore {

  private static final Map<String, IndicatorCache> store = new ConcurrentHashMap<>();

  public static void put(String key, IndicatorCache cache) {
    store.put(key, cache);
  }

  public static IndicatorCache get(String key) {
    return store.get(key);
  }

  public static Map<String, IndicatorCache> getAll() {
    return store;
  }

  public static void clear() {
    store.clear();
  }
}
