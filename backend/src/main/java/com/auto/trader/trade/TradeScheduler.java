package com.auto.trader.trade;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeScheduler {

    //private final TradeService tradeService;

    // 10초마다 실행 예시
    @Scheduled(fixedDelay = 10000)
    public void checkTradeConditions() {
        log.info("🔄 포지션 조건 검사 시작");
        try {
            //tradeService.checkAndExecute();
        } catch (Exception e) {
            log.error("🚨 거래 실행 중 오류 발생", e);
        }
    }
}
