package com.ayno.aynobe.config.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaPathGenerator {
    @Value("${media.s3.privatePrefix}") private String privatePrefix;
    @Value("${media.s3.publicPrefix}")  private String publicPrefix;

    public String makeUnboundBaseKey(long userId, String scope, String uuid, String ext) {
        return "user/%d/uploads/%s/%s/original.%s".formatted(userId, scope, uuid, ext);
    }

    public String toPrivateKey(String baseKey) {
        return privatePrefix + baseKey;
    }

    public String toPublicKey(String baseKey)  {
        return publicPrefix  + baseKey;
    }
}
