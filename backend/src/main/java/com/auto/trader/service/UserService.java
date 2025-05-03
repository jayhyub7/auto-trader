package com.auto.trader.service;

import com.auto.trader.domain.User;
import com.auto.trader.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public void deleteByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
    }

    public User updateNickName(String email, String newNickName) {
        return userRepository.findByEmail(email)
            .map(user -> {
                user.setNickName(newNickName);
                return userRepository.save(user);
            })
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    

}
