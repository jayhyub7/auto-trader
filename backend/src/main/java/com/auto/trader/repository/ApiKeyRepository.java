package com.auto.trader.repository;

import com.auto.trader.domain.ApiKey;
import com.auto.trader.domain.Exchange;
import com.auto.trader.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {    
    List<ApiKey> findAllByUser(User user);
    Optional<ApiKey> findByUserAndExchange(User user, Exchange exchange);
    void deleteByUserAndExchange(User user, Exchange exchange);
}
