package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.config.util.ImageResizer;
import com.ayno.aynobe.config.util.MediaPathGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * "발행" 시점에:
 *  1) private 경로의 원본 존재 확인
 *  2) 필요한 파생본(이미지: w320/w800/w1600 · 오디오는 당장 원본을 그대로 복사(임시)만 두고, 나중에 ffmpeg 연결 시 메서드만 교체
 *  3) 원본+파생을 public 경로로 복사
 */
@Service
@RequiredArgsConstructor
public class MediaVariantService {

    private final S3Client s3Client;
    private final ImageResizer imageResizer;
    private final MediaPathGenerator path;

    @Value("${media.s3.bucket}") private String bucket;

    private static final Set<String> IMAGE = Set.of("jpg","jpeg","png","webp");
    private static final Set<String> AUDIO = Set.of("mp3","m4a","wav");

    /**
     * 발행 시 호출: baseKey(…/original.ext)를 기준으로
     * 1) private 원본 확인 → 2) 파생 생성 → 3) public 복사
     */
    public void publishOne(String baseKey) {
        String ext = extOf(baseKey);
        String originalPrivate = path.toPrivateKey(baseKey);

        // 0) 원본이 없으면 발행 불가
        requireExists(originalPrivate);

        if (IMAGE.contains(ext)) {
            // 1) 이미지 파생(w320/w800/w1600) → 없는 것만 만들어 둔다
            ensureImageVariants(originalPrivate);

            // 2) public으로 복사 (원본 + 파생)
            copyToPublic(baseKey, List.of("original." + ext, "w320.jpg", "w800.jpg", "w1600.jpg"));
        } else if (AUDIO.contains(ext)) {
            // 임시: 오디오는 ffmpeg 붙이기 전까지 원본만 public 복사
            copyToPublic(baseKey, List.of("original." + ext));
        } else {
            throw CustomException.badRequest("지원하지 않는 확장자: " + ext);
        }
    }

    /**
     * public 사본(원본 + 파생) 일괄 삭제.
     * - 존재 유무 미리 확인하지 않고 바로 batch delete 요청
     * - S3는 없는 키를 지우더라도 에러 없이 처리(Deleted/Errors 로만 리포트)하므로 안전
     */
    public void deletePublicCopies(String baseKey) {
        String ext = extOf(baseKey);                       // ex) "png"
        String publicDir = dirPrefix(path.toPublicKey(baseKey)); // ".../prod/public/.../media/<uuid>/"

        // 원본 파일명 + 파생 파일명 목록
        List<String> names = new ArrayList<>();
        names.add("original." + ext);

        if (IMAGE.contains(ext)) {
            names.addAll(List.of("w320.jpg", "w800.jpg", "w1600.jpg"));
        } else if (AUDIO.contains(ext)) {
            // 오디오는 정책에 맞춰 추가/수정
            names.addAll(List.of("a128.mp3", "preview30.mp3"));
        } else {
            // 지원하지 않는 확장자는 조용히 무시하거나 예외로 바꿔도 된다
            return;
        }

        // S3 object identifiers 구성
        List<ObjectIdentifier> objs = names.stream()
                .map(n -> ObjectIdentifier.builder().key(publicDir + n).build())
                .toList();

        if (!objs.isEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(objs).build())
                    .build());
        }
    }

    /* =========== 이미지 파생 생성 =========== */

    private void ensureImageVariants(String originalPrivateKey) {
        createIfMissing(originalPrivateKey, "w320.jpg", 320);
        createIfMissing(originalPrivateKey, "w800.jpg", 800);
        createIfMissing(originalPrivateKey, "w1600.jpg", 1600);
    }

    /**
     * variantName(w800.jpg) 파일이 없으면
     * - 원본 바이트를 가져와서
     * - targetWidth로 리사이즈 후
     * - private 경로에 저장한다.
     */
    private void createIfMissing(String originalPrivateKey, String variantName, int targetWidth) {
        String variantPrivateKey = replaceTail(originalPrivateKey, variantName);
        if (exists(variantPrivateKey)) return; // 이미 있으면 끝

        byte[] original = getBytes(originalPrivateKey);
        byte[] resizedJpeg = imageResizer.resizeSet(original, List.of(targetWidth)).get(targetWidth);

        putBytes(variantPrivateKey, resizedJpeg, "image/jpeg");
    }

    /* =========== public 복사 =========== */

    /**
     * 주어진 파일명 목록(원본/파생)을 private → public로 복사.
     * - baseKey: …/original.ext (prefix 제외)
     * - fileNames: ["original.jpg", "w320.jpg"…]
     */
    private void copyToPublic(String baseKey, List<String> fileNames) {
        String privatePrefix = path.toPrivateKey(baseKey).replaceAll("/original\\.[^.]+$", "/");
        String publicPrefix  = path.toPublicKey(baseKey).replaceAll("/original\\.[^.]+$", "/");

        for (String name : fileNames) {
            String src = privatePrefix + name;
            String dst = publicPrefix  + name;
            if (exists(src)) {
                s3Client.copyObject(c -> c.sourceBucket(bucket).sourceKey(src)
                        .destinationBucket(bucket).destinationKey(dst));
            }
        }
    }

    /* =========== S3 helper =========== */

    private void requireExists(String key) {
        s3Client.headObject(h -> h.bucket(bucket).key(key)); // 존재하지 않으면 404 예외
    }

    private boolean exists(String key) {
        try { requireExists(key); return true; }
        catch (S3Exception e) { return e.statusCode() != 404 ? sneaky(e) : false; }
    }

    private byte[] getBytes(String key) {
        try (var in = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            return in.readAllBytes();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    private void putBytes(String key, byte[] bytes, String contentType) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(contentType)
                        // 파일명이 바뀌지 않는 한 불변 → 캐시 오래
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromBytes(bytes));
    }

    /* =========== 문자열 유틸 =========== */

    /** .../original.ext → .../<variantFileName> */
    private static String replaceTail(String originalKey, String variantFileName) {
        return originalKey.replaceAll("/original\\.[^.]+$", "/" + variantFileName);
    }

    /** baseKey의 확장자 (점 제외, 소문자) */
    private static String extOf(String baseKey) {
        int dot = baseKey.lastIndexOf('.');
        return (dot > -1) ? baseKey.substring(dot + 1).toLowerCase() : "";
    }

    private static String dirPrefix(String keyWithOriginal) {
        // 끝의 "/original.xxx" 만 잘라내고 뒤에 "/" 유지
        return keyWithOriginal.replaceAll("/original\\.[^.]+$", "/");
    }

    private static boolean sneaky(RuntimeException e) { throw e; }
    private static boolean sneaky(S3Exception e) { throw e; }
}