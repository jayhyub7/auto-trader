package com.auto.trader.position.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auto.trader.position.entity.IndicatorTypeEntity;

public interface IndicatorTypeRepository extends JpaRepository<IndicatorTypeEntity, Long> {

}
