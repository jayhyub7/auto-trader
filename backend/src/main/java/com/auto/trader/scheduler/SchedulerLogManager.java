package com.auto.trader.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.auto.trader.scheduler.enums.SchedulerType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerLogManager {

	private final SchedulerStatusCache statusCache;
	private final SchedulerType type;
	private final StringBuilder logBuilder = new StringBuilder();

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
			logBuilder.append(timestamp()).append(" ").append(message).append("\n");
		}
	}

	public void log(String format, Object... args) {
		if (isLogEnabled()) {
			log.info(format, args);
			String formatted = String.format(format, args);
			logBuilder.append(timestamp()).append(" ").append(formatted).append("\n");
		}
	}

	public String getLogText() {
		return logBuilder.toString();
	}

	public void clear() {
		logBuilder.setLength(0);
	}

	private String timestamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
	}
}
