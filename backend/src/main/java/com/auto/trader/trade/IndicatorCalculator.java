package com.auto.trader.trade;

import com.auto.trader.trade.dto.CandleDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class IndicatorCalculator {

    public void calculateAndStore(String symbol, String timeframe, List<CandleDto> candles) {
        try {
            if (candles.size() < 50) return; // 안정성 확보를 위해 최소 길이 보장

            List<IndicatorUtil.IndicatorPoint> rsi = IndicatorUtil.calculateRSI(candles, 14);
            List<IndicatorUtil.DualIndicatorPoint> stoch = IndicatorUtil.calculateStochRSI(candles, 14, 3);
            IndicatorUtil.VWBB vwbb = IndicatorUtil.calculateVWBB(candles, 20, 2);

            IndicatorCache cache = new IndicatorCache(candles, rsi, stoch, vwbb);
            String key = symbol + "_" + timeframe;
            IndicatorMemoryStore.put(key, cache);
            /*
            log.debug("📊 [{}] 지표 저장 완료: RSI {}, Stoch {}, VWBB.Basis {}",
                    key,
                    rsi.get(rsi.size() - 1).getValue(),
                    stoch.get(stoch.size() - 1).getK(),
                    vwbb.getBasis().get(vwbb.getBasis().size() - 1).getValue()
            );
            */
        } catch (Exception e) {
            log.error("❌ 지표 계산 실패 [{}_{}]", symbol, timeframe, e);
        }
    }
}
