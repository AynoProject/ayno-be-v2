package com.ayno.aynobe.config.security.service;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.config.security.oauth.OAuthAttributes;
import com.ayno.aynobe.entity.LinkedAccount;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.AgeBand;
import com.ayno.aynobe.entity.enums.GenderType;
import com.ayno.aynobe.entity.enums.UsageDepthType;
import com.ayno.aynobe.repository.LinkedAccountRepository;
import com.ayno.aynobe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final LinkedAccountRepository linkedAccountRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        OAuthAttributes attributes = OAuthAttributes.of(provider, oAuth2User.getAttributes());

        // DB 저장 or 조회
        LinkedAccount linked = linkedAccountRepository.findWithUser(
                        attributes.getProvider(),
                        attributes.getProviderId()
                )
                .orElseGet(() -> {
                    // 이메일을 username으로 사용(필수 동의 전제)
                    String username = attributes.getEmail();
                    if (username == null || username.isBlank()) {
                        throw new OAuth2AuthenticationException("이메일 동의가 필요합니다.");
                    }

                    User user = userRepository.findByUsername(username)
                            .orElseGet(() -> userRepository.save(
                                    User.builder()
                                            .username(username)
                                            .passwordHash(null) // 소셜-only
                                            .profileImageUrl(attributes.getPicture())
                                            .gender(GenderType.NONE)
                                            .aiUsageDepth(UsageDepthType.NONE)
                                            .ageBand(AgeBand.NONE)
                                            .build()
                            ));

                    LinkedAccount la = LinkedAccount.builder()
                            .provider(attributes.getProvider())
                            .providerId(attributes.getProviderId())
                            .user(user)
                            .build();
                    return linkedAccountRepository.save(la);
                });

        return new CustomUserDetails(linked.getUser());
    }
}