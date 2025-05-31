package com.auto.trader.executedreport.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.trader.domain.User;
import com.auto.trader.executedreport.dto.ExecutedReportResponseDto;
import com.auto.trader.executedreport.service.ExecutedReportService;
import com.auto.trader.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/executed-reports")
@RequiredArgsConstructor
public class ExecutedReportController {

	private final ExecutedReportService executedReportService;
	private final UserService userService;

	@GetMapping
	public List<ExecutedReportResponseDto> getUserExecutedReports(@AuthenticationPrincipal OAuth2User principal) {
		String email = (String) principal.getAttributes().get("email");
		User user = userService.findByEmail(email).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
		return executedReportService.getUserExecutedReports(user);
	}
}