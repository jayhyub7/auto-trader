// 파일: com.auto.trader.scheduler.config.SchedulerLogManagerConfig.java

package com.auto.trader.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.scheduler.SchedulerStatusCache;
import com.auto.trader.scheduler.enums.SchedulerType;

@Configuration
public class SchedulerLogManagerConfig {

	@Bean
	public SchedulerLogManager entryLogManager(SchedulerStatusCache cache) {
		return new SchedulerLogManager(cache, SchedulerType.ENTRY);
	}

	@Bean
	public SchedulerLogManager exitLogManager(SchedulerStatusCache cache) {
		return new SchedulerLogManager(cache, SchedulerType.EXIT);
	}

	@Bean
	public SchedulerLogManager balanceLogManager(SchedulerStatusCache cache) {
		return new SchedulerLogManager(cache, SchedulerType.BALANCE);
	}

	@Bean
	public SchedulerLogManager indicatorLogManager(SchedulerStatusCache cache) {
		return new SchedulerLogManager(cache, SchedulerType.INDICATOR);
	}

	@Bean
	public SchedulerLogManager fxLogManager(SchedulerStatusCache cache) {
		return new SchedulerLogManager(cache, SchedulerType.FX);
	}
}
