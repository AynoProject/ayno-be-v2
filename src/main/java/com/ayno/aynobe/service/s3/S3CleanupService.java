package com.ayno.aynobe.service.s3;

import com.ayno.aynobe.repository.ArtifactMediaRepository;
import com.ayno.aynobe.repository.StepSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3CleanupService {

    private final S3Client s3Client;
    private final ArtifactMediaRepository artifactMediaRepository;
    private final StepSectionRepository stepSectionRepository; // ★ 워크플로우 미디어용

    @Value("${media.s3.bucket}")
    private String bucket;

    @Value("${media.s3.privatePrefix}")
    private String privatePrefix; // ★ private 경로 주입

    private static final int BATCH_DELETE_SIZE = 1000; // S3 API는 최대 1000개

    /**
     * 매일 새벽 3시에 고아 S3 파일 정리
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(readOnly = true)
    public void cleanupOrphanS3Files() {
        log.info("[BATCH] S3 고아 파일 정리 작업 시작...");
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS); // 24시간 이전 파일
        String scanPrefix = "user/"; // "user/uploads/" 경로만 스캔

        List<ObjectIdentifier> filesToDeleteBatch = new ArrayList<>();

        try {
            ListObjectsV2Iterable s3ObjectPages = s3Client.listObjectsV2Paginator(
                    req -> req.bucket(bucket).prefix(scanPrefix));

            for (var page : s3ObjectPages) {

                // 1. S3 페이지(최대 1000개)에서 1차 필터링
                List<S3Object> candidates = page.contents().stream()
                        .filter(obj -> obj.lastModified().isBefore(cutoff))
                        .filter(obj -> obj.key().contains("/original.")) // 원본 파일만
                        .toList();

                if (candidates.isEmpty()) {
                    continue; // 이 페이지는 검사할 대상 없음
                }

                // 2. S3 Key -> baseKey 리스트로 변환
                List<String> baseKeysInBatch = candidates.stream()
                        .map(S3Object::key)       // 1. S3Object -> String (s3Key)
                        .map(this::s3KeytoBaseKey) // 2. String (s3Key) -> String (baseKey)
                        .filter(java.util.Objects::nonNull)
                        .toList();

                if (baseKeysInBatch.isEmpty()) {
                    continue;
                }

                // 3. (★성능개선★) DB에 존재하는 키만 "한번에" 조회 (쿼리 1번)
                Set<String> existingKeysInDb = artifactMediaRepository.findBaseKeysIn(baseKeysInBatch);
                // (워크플로우 미디어도 있다면)
                existingKeysInDb.addAll(stepSectionRepository.findBaseKeysIn(baseKeysInBatch));

                // 4. S3 목록과 DB 목록 비교
                for (S3Object candidate : candidates) {
                    String baseKey = s3KeytoBaseKey(candidate.key());

                    // DB에 존재하지 않으면(false) -> 고아 파일
                    if (baseKey != null && !existingKeysInDb.contains(baseKey)) {
                        log.warn("[BATCH] 고아 파일 발견, 삭제 예정: {}", candidate.key());
                        filesToDeleteBatch.add(ObjectIdentifier.builder().key(candidate.key()).build());
                    }
                }

                // 5. 1000개 모이면 S3에서 삭제
                if (filesToDeleteBatch.size() >= BATCH_DELETE_SIZE) {
                    deleteBatchFromS3(filesToDeleteBatch);
                    filesToDeleteBatch.clear();
                }
            }

            // 6. 남은 배치 삭제
            if (!filesToDeleteBatch.isEmpty()) {
                deleteBatchFromS3(filesToDeleteBatch);
            }

        } catch (Exception e) {
            log.error("[BATCH] S3 고아 파일 정리 중 오류 발생", e);
        }
        log.info("[BATCH] S3 고아 파일 정리 작업 완료.");
    }

    /**
     * S3의 전체 Key("prod/private/user/...") -> DB의 baseKey("user/...")로 변환
     */
    private String s3KeytoBaseKey(String s3Key) {
        if (s3Key != null && s3Key.startsWith(privatePrefix)) {
            // "prod/private/" 부분을 잘라내고 "user/..." 부터 반환
            return s3Key.substring(privatePrefix.length());
        }
        return null;
    }

    /**
     * S3에서 파일 묶음 삭제
     */
    private void deleteBatchFromS3(List<ObjectIdentifier> keys) {
        if (keys.isEmpty()) return;

        log.info("[BATCH] S3 파일 {}개 삭제 실행...", keys.size());
        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(keys).build())
                .build());
    }
}
