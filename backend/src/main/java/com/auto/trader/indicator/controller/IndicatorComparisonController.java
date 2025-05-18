// 파일: com.auto.trader.indicator.controller.IndicatorComparisonController.java

package com.auto.trader.indicator.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.indicator.dto.AllComparisonResultDto;
import com.auto.trader.indicator.dto.RsiComparisonRequestDto;
import com.auto.trader.indicator.service.IndicatorComparisonService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/indicator")
@RequiredArgsConstructor
public class IndicatorComparisonController {

	private final IndicatorComparisonService indicatorComparisonService;

	@PostMapping("/compare-all")
	public AllComparisonResultDto compareAll(@RequestBody RsiComparisonRequestDto request) {
		return indicatorComparisonService.compareAllIndicators(request);
	}
}