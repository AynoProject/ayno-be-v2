package com.ayno.aynobe.config.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaPathGenerator {
    @Value("${media.s3.privatePrefix}") private String privatePrefix;

    public String makeArtifactBaseKey(long artifactId, String uuid, String ext) {
        return "artifact/%d/media/%s/original.%s".formatted(artifactId, uuid, ext);
    }

    public String makeSectionBaseKey(long workflowId, long sectionId, String uuid, String ext) {
        return "workflow/%d/section/%d/media/%s/original.%s"
                .formatted(workflowId, sectionId, uuid, ext);
    }

    /** S3에 올릴 실제 키(프리픽스 + baseKey) */
    public String toPrivateKey(String baseKey) {
        return privatePrefix + baseKey;
    }
}
