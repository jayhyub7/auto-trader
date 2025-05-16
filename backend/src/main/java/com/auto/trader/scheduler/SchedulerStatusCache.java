// 📄 src/main/java/com/auto/trader/scheduler/SchedulerStatusCache.java

package com.auto.trader.scheduler;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.auto.trader.scheduler.enums.SchedulerType;
import com.auto.trader.scheduler.repository.SchedulerStatusRepository;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchedulerStatusCache {
	private final SchedulerStatusRepository repository;

	@Data
	@AllArgsConstructor
	public static class Status {
		private boolean enabled;
		private boolean log;
	}

	@PostConstruct
	public void loadFromDB() {
		repository.findAll().forEach(status -> {
			statusMap.put(status.getType(), new Status(status.isEnabled(), status.isLogEnabled()));
		});

		// 만약 DB에 없으면 기본값 등록
		for (SchedulerType type : SchedulerType.values()) {
			statusMap.putIfAbsent(type, new Status(true, true));
		}
	}

	private final Map<SchedulerType, Status> statusMap = new EnumMap<>(SchedulerType.class);

	public Map<SchedulerType, Status> getStatusMap() {
		return statusMap;
	}

	public void updateStatus(SchedulerType type, boolean enabled, boolean log) {
		statusMap.put(type, new Status(enabled, log));
	}
}
