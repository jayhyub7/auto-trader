package com.auto.trader.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.auto.trader.domain.Role;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oAuth2User;
    private final Role role;

    public CustomOAuth2User(OAuth2User oAuth2User, Role role) {
        this.oAuth2User = oAuth2User;
        this.role = role;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return oAuth2User.getName();
    }

    public String getEmail() {
        return (String) oAuth2User.getAttributes().get("email");
    }

    public Role getRole() {
        return role;
    }
}