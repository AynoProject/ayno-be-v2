package com.ayno.aynobe.config.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaPathGenerator {
    @Value("${media.s3.privatePrefix}") private String privatePrefix;
    @Value("${media.s3.publicPrefix}")  private String publicPrefix;

    public String makeArtifactBaseKey(long artifactId, String uuid, String ext) {
        return "artifact/%d/media/%s/original.%s".formatted(artifactId, uuid, ext);
    }

    public String makeSectionBaseKey(long workflowId, long sectionId, String uuid, String ext) {
        return "workflow/%d/section/%d/media/%s/original.%s"
                .formatted(workflowId, sectionId, uuid, ext);
    }

    public String toPrivateKey(String baseKey) {
        return privatePrefix + baseKey;
    }

    public String toPublicKey(String baseKey)  {
        return publicPrefix  + baseKey;
    }
}
