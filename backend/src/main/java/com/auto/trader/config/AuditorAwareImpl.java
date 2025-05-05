package com.auto.trader.config;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {    	
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        // principal이 UserDetails를 구현한 객체라면 캐스팅 후 email 반환
        Object principal = auth.getPrincipal();
  
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            String email = (String) oAuth2User.getAttributes().get("email");       
            return Optional.of(email);
        }

        // 아니면 문자열 형태로 반환
        return Optional.of(principal.toString());
    }
}
