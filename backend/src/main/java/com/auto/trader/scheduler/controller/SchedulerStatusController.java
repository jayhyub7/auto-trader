// 📁 com.auto.trader.scheduler.controller.SchedulerStatusController.java

package com.auto.trader.scheduler.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.scheduler.SchedulerStatusCache;
import com.auto.trader.scheduler.dto.SchedulerStatusDto;
import com.auto.trader.scheduler.entity.SchedulerStatus;
import com.auto.trader.scheduler.enums.SchedulerType;
import com.auto.trader.scheduler.repository.SchedulerStatusRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schedulers")
@RequiredArgsConstructor
public class SchedulerStatusController {

	private final SchedulerStatusRepository repository;
	private final SchedulerStatusCache cache;

	// ✅ 전체 조회
	@GetMapping
	public List<SchedulerStatusDto> getAllStatuses() {
		return cache
			.getStatusMap()
			.entrySet()
			.stream()
			.map(entry -> SchedulerStatusDto
				.builder()
				.type(entry.getKey())
				.enabled(entry.getValue().isEnabled())
				.log(entry.getValue().isLog())
				.build())
			.collect(Collectors.toList());
	}

	// ✅ 개별 수정
	@PostMapping("/{type}")
	public void updateStatus(@PathVariable(name = "type") SchedulerType type, @RequestBody SchedulerStatusDto dto) {
		cache.updateStatus(type, dto.isEnabled(), dto.isLog());
		repository.save(new SchedulerStatus(type, dto.isEnabled(), dto.isLog()));
	}
}
