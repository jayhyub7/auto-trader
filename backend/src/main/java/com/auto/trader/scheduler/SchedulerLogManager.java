// 📄 src/main/java/com/auto/trader/scheduler/SchedulerLogManager.java

package com.auto.trader.scheduler;

import org.springframework.stereotype.Component;

import com.auto.trader.scheduler.enums.SchedulerType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchedulerLogManager {

	private final SchedulerStatusCache statusCache;

	public boolean isLogEnabled(SchedulerType schedulerType) {
		return statusCache
			.getStatusMap()
			.getOrDefault(schedulerType, new SchedulerStatusCache.Status(false, false))
			.isLog();
	}

	public void log(SchedulerType schedulerType, Runnable logAction) {
		if (isLogEnabled(schedulerType)) {
			logAction.run();
		}
	}
}
