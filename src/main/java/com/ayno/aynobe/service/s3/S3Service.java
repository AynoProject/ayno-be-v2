package com.ayno.aynobe.service.s3;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.config.util.MediaPathGenerator;
import com.ayno.aynobe.dto.asset.UploadDeleteRequestDTO;
import com.ayno.aynobe.dto.asset.UploadPresignRequestDTO;
import com.ayno.aynobe.dto.asset.UploadPresignResponseDTO;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.repository.ArtifactRepository;
import com.ayno.aynobe.repository.StepSectionRepository;
import com.ayno.aynobe.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Set<String> IMG = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> AUD = Set.of("mp3", "m4a", "wav");

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final MediaPathGenerator pathGen;

    private final ArtifactRepository artifactRepository;
    private final WorkflowRepository workflowRepository;
    private final StepSectionRepository stepSectionRepository;

    @Value("${media.s3.bucket}")
    private String bucket;

    public UploadPresignResponseDTO createPresign(User user, UploadPresignRequestDTO req) {
        validate(req);

        String uuid = UUID.randomUUID().toString();

        String baseKey = pathGen.makeUnboundBaseKey(
                user.getUserId(),
                req.getScope().toString().toLowerCase(),
                uuid,
                req.getExt()
        );

        String s3Key = pathGen.toPrivateKey(baseKey);
        String contentType = mimeFromExt(req.getExt());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = presigner.presignPutObject(putObjectPresignRequest);
        URL url = presignedPutObjectRequest.url();

        return UploadPresignResponseDTO.builder()
                .baseKey(baseKey)
                .putUrl(url.toString())
                .contentType(contentType)
                .build();
    }

    @Transactional(readOnly = true)
    public void deletePrivateObject(User user, UploadDeleteRequestDTO req) {
        String baseKey = req.getBaseKey();

        // --- 권한 검증 ---
        Long ownerId = parseUserIdFromBaseKey(baseKey);
        if (ownerId == null) {
            throw CustomException.badRequest("유효하지 않은 baseKey 형식입니다.");
        }

        if (!ownerId.equals(user.getUserId())) {
            throw CustomException.forbidden("파일 삭제 권한이 없습니다.");
        }

        // --- 삭제 ---
        this.deleteS3MediaSet(req.getBaseKey());
    }

    public void deleteS3MediaSet(String baseKey) {
        if (baseKey == null || baseKey.isBlank()) {
            return;
        }

        String ext = extOf(baseKey); // ".png" 등 확장자 추출 (기존 private 메소드 활용)
        String privateDir = toPrivateDirPrefix(baseKey); // ".../prod/private/.../media/<uuid>/" (기존 private 메소드 활용)

        List<String> names = new ArrayList<>();
        names.add("original." + ext);
        if (IMG.contains(ext)) {
            names.addAll(List.of("w320.jpg", "w800.jpg", "w1600.jpg"));
        }
        // TODO: 만약 AUDIO 파생 파일이 있다면 여기에 추가 (예: "preview.mp3")

        var objects = names.stream()
                .map(n -> ObjectIdentifier.builder().key(privateDir + n).build())
                .toList();

        if (!objects.isEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(objects).build())
                    .build());
        }
    }

    private void validate(UploadPresignRequestDTO r) {
        String ext = r.getExt().toLowerCase();
        if (!IMG.contains(ext) && !AUD.contains(ext)) {
            throw CustomException.badRequest("지원하지 않는 확장자: " + ext);
        }

        long max = IMG.contains(ext) ? 10_000_000L : 50_000_000L; // 이미지 10MB / 오디오 50MB
        if (r.getBytes() > max) {
            throw CustomException.badRequest("파일이 너무 큽니다. 최대 " + (IMG.contains(ext) ? "10MB" : "50MB"));
        }
    }

    private String mimeFromExt(String ext) {
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "mp3" -> "audio/mpeg";
            case "m4a" -> "audio/mp4";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }

    private Long parseUserIdFromBaseKey(String baseKey) {
        try {
            if (baseKey.startsWith("user/")) {
                String[] parts = baseKey.split("/");
                return Long.parseLong(parts[1]);
            }
        } catch (Exception e) {
            // 파싱 실패
        }
        return null;
    }

    private String toPrivateDirPrefix(String baseKey) {
        String keyWithOriginal = pathGen.toPrivateKey(baseKey);      // ".../private/.../original.ext"
        return keyWithOriginal.replaceAll("/original\\.[^.]+$", "/"); // ".../private/.../"
    }

    private static String extOf(String baseKey) {
        int dot = baseKey.lastIndexOf('.');
        return (dot > -1) ? baseKey.substring(dot + 1).toLowerCase() : "";
    }
}
