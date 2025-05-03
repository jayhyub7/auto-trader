package com.auto.trader.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.auto.trader.domain.Role;
import com.auto.trader.domain.User;
import com.auto.trader.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String sub = String.valueOf(attributes.get("sub"));
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");
        String picture = (String) attributes.get("picture");

        if (!(Boolean) attributes.get("email_verified")) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request", "이메일 인증 실패", ""));
        }

        User user = userRepository.findByEmail(email).orElseGet(() ->
            userRepository.save(User.builder()
                .sub(sub)
                .email(email)
                .name(name)
                .givenName(givenName)
                .familyName(familyName)
                .picture(picture)
                .provider(provider)
                .role(Role.USER)
                .build())
        );

        boolean changed = false;
        if (!name.equals(user.getName())) { user.setName(name); changed = true; }
        if (!givenName.equals(user.getGivenName())) { user.setGivenName(givenName); changed = true; }
        if (!familyName.equals(user.getFamilyName())) { user.setFamilyName(familyName); changed = true; }
        if (!picture.equals(user.getPicture())) { user.setPicture(picture); changed = true; }
        if (changed) userRepository.save(user);

        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
            attributes,
            "email"
        );
    }
}