package com.auto.trader.adminkey.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auto.trader.adminkey.entity.AdminKey;
import com.auto.trader.domain.Exchange;

public interface AdminKeyRepository extends JpaRepository<AdminKey, Long> {
    Optional<AdminKey> findByExchange(Exchange exchange);
    void deleteByExchange(Exchange exchange);
}
