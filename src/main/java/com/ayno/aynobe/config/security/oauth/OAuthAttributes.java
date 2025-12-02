package com.ayno.aynobe.config.security.oauth;

import com.ayno.aynobe.config.exception.CustomException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Getter
@Builder
@AllArgsConstructor
public class OAuthAttributes {

    private final String provider;   // "google" | "kakao"
    private final String providerId; // Google: sub, Kakao: id
    private final String email;      // 필수 동의 전제
    private String picture;

    public static OAuthAttributes of(String provider, Map<String, Object> attributes) {
        String id = Optional.ofNullable(provider).orElse("").trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "google" -> ofGoogle(id, attributes);
            case "kakao"  -> ofKakao(id, attributes);
            default -> throw CustomException.notFound("Unknown provider: " + id);
        };
    }

    private static OAuthAttributes ofGoogle(String provider, Map<String, Object> attrs) {
        Object sub = attrs.get("sub");
        if (sub == null) throw CustomException.badRequest("Google 응답에 sub가 없습니다");
        Object email = attrs.get("email");
        if (email == null || email.toString().isBlank()) throw CustomException.badRequest("Google 이메일 동의가 필요합니다");

        return OAuthAttributes.builder()
                .provider(provider)
                .providerId(sub.toString())
                .email(email.toString().toLowerCase())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String provider, Map<String, Object> attrs) {
        Object idObj = attrs.get("id");
        if (idObj == null)
            throw CustomException.badRequest("Kakao 응답에 id가 없습니다");

        Map<String, Object> account = (Map<String, Object>) attrs.getOrDefault("kakao_account", Map.of());
        Object email = account.get("email");
        if (email == null || email.toString().isBlank())
            throw CustomException.badRequest("Kakao 이메일 동의가 필요합니다");

        Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
        String pictureUrl = (String) profile.get("profile_image_url");

        return OAuthAttributes.builder()
                .provider(provider)
                .providerId(idObj.toString())
                .email(email.toString().toLowerCase())
                .picture(pictureUrl)
                .build();
    }
}
