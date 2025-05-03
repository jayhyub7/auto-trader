package com.auto.trader.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auto.trader.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findById(Long id);
	Optional<User> findByEmail(String email);
}
