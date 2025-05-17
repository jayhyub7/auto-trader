// 파일: com.auto.trader.balance.scheduler.BalanceCacheScheduler.java

package com.auto.trader.balance.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.auto.trader.balance.dto.BalanceDto;
import com.auto.trader.balance.dto.ExchangeBalanceDto;
import com.auto.trader.balance.dto.cache.BalanceMemoryStore;
import com.auto.trader.balance.service.CurrentBalanceService;
import com.auto.trader.domain.User;
import com.auto.trader.repository.UserRepository;
import com.auto.trader.scheduler.SchedulerLogManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BalanceCacheScheduler {

	private final UserRepository userRepository;
	private final CurrentBalanceService currentBalanceService;
	private final SchedulerLogManager balanceLogManager;

	public BalanceCacheScheduler(UserRepository userRepository, CurrentBalanceService currentBalanceService,
			@Qualifier("balanceLogManager") SchedulerLogManager balanceLogManager) {
		this.userRepository = userRepository;
		this.currentBalanceService = currentBalanceService;
		this.balanceLogManager = balanceLogManager;
	}

	@Scheduled(fixedDelay = 60000)
	public void updateBalances() {
		if (!balanceLogManager.isEnabled())
			return;

		List<User> users = userRepository.findAll();
		for (User user : users) {
			try {
				List<ExchangeBalanceDto> exchangeBalances = currentBalanceService.getBalances(user);
				List<BalanceDto> allBalances = new ArrayList<>();

				for (ExchangeBalanceDto exchangeBalance : exchangeBalances) {
					if (exchangeBalance.getBalances() != null) {
						allBalances.addAll(exchangeBalance.getBalances());
					}
				}

				BalanceMemoryStore.put(user.getId(), allBalances);
				balanceLogManager.log("✅ 잔고 캐시 갱신 완료: userId={}", user.getId());
			} catch (Exception e) {
				log.error("❌ 잔고 캐시 갱신 실패: userId=" + user.getId(), e);
			}
		}
	}
}