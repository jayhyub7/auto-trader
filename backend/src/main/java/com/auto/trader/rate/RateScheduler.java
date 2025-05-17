// 파일: com.auto.trader.rate.RateScheduler.java

package com.auto.trader.rate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auto.trader.scheduler.SchedulerLogManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateScheduler {

	private final RateService rateService;
	private final SchedulerLogManager fxLogManager;

	public RateScheduler(RateService rateService, @Qualifier("fxLogManager") SchedulerLogManager fxLogManager) {
		this.rateService = rateService;
		this.fxLogManager = fxLogManager;

		// 최초 1회 실행
		fetchRates();
	}

	@Scheduled(fixedDelay = 20 * 60 * 1000) // 20분
	public void fetchRates() {
		if (!fxLogManager.isEnabled())
			return;

		fxLogManager.log("💱 환율 정보 갱신 시작");
		try {
			rateService.updateRates();
		} catch (Exception e) {
			log.error("🚨 환율 갱신 실패", e);
		}
	}
}
