package com.auto.trader.rate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateScheduler {

    private final RateService rateService;

    // 매 10분마다 실행 (초 분 시 * * *)
    //@Scheduled(fixedDelay = 5000)
    public void fetchRates() {
        log.info("💱 환율 정보 갱신 시작");
        try {
            rateService.updateRates();
        } catch (Exception e) {
            log.error("🚨 환율 갱신 실패", e);
        }
    }
}
