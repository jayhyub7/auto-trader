package com.auto.trader.trade.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.trade.entity.TradeCondition;
import com.auto.trader.trade.repository.TradeConditionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeConditionService {

    private final TradeConditionRepository tradeConditionRepository;
  
    @Transactional
    public void saveAll(List<TradeCondition> conditions) {
        tradeConditionRepository.saveAll(conditions);
    }
}