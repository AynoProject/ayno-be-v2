package com.ayno.aynobe.config.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {
    @Bean
    S3Presigner s3Presigner(@Value("${media.s3.region}") String region) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build(); // 자격증명은 기본 프로바이더 체인 사용
    }

    @Bean
    S3Client s3Client(@Value("${media.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build(); // 자격증명은 기본 프로바이더 체인 사용
    }
}