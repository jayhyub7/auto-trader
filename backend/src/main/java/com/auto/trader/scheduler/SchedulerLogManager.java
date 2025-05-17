// 파일: com.auto.trader.scheduler.SchedulerLogManager.java

package com.auto.trader.scheduler;

import com.auto.trader.scheduler.enums.SchedulerType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerLogManager {

	private final SchedulerStatusCache statusCache;
	private final SchedulerType type;

	public SchedulerLogManager(SchedulerStatusCache statusCache, SchedulerType type) {
		this.statusCache = statusCache;
		this.type = type;
	}

	public boolean isEnabled() {
		return statusCache.getStatusMap().getOrDefault(type, new SchedulerStatusCache.Status(false, false)).isEnabled();
	}

	public boolean isLogEnabled() {
		return statusCache.getStatusMap().getOrDefault(type, new SchedulerStatusCache.Status(false, false)).isLog();
	}

	public void log(String message) {

		if (isLogEnabled()) {
			log.info(message);
		}
	}

	public void log(String format, Object... args) {

		if (isLogEnabled()) {
			log.info(format, args);
		}
	}
}