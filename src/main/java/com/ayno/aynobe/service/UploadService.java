package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.config.util.MediaPathGenerator;
import com.ayno.aynobe.dto.asset.UploadPresignRequestDTO;
import com.ayno.aynobe.dto.asset.UploadPresignResponseDTO;
import com.ayno.aynobe.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final Set<String> IMG = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> AUD = Set.of("mp3", "m4a", "wav");

    private final S3Presigner presigner;
    private final MediaPathGenerator pathGen;

    @Value("${media.s3.bucket}")
    private String bucket;

    public UploadPresignResponseDTO createPresign(User user, UploadPresignRequestDTO req) {
        validate(req);

        String uuid = UUID.randomUUID().toString();

        String baseKey = switch (req.getScope()) {
            case ARTIFACT -> pathGen.makeArtifactBaseKey(req.getArtifactId(), uuid, req.getExt());
            case SECTION  -> pathGen.makeSectionBaseKey(req.getWorkflowId(), req.getSectionId(), uuid, req.getExt());
        };

        String s3Key = pathGen.toPrivateKey(baseKey);
        String contentType = mimeFromExt(req.getExt());

        PutObjectRequest putObject = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObject)
                .build();

        PresignedPutObjectRequest ppo = presigner.presignPutObject(presign);
        URL url = ppo.url();

        return UploadPresignResponseDTO.builder()
                .baseKey(baseKey)
                .putUrl(url.toString())
                .contentType(contentType)
                .build();
    }

    private void validate(UploadPresignRequestDTO r) {
        if (r.getScope() == UploadPresignRequestDTO.Scope.ARTIFACT && r.getArtifactId() == null) {
            throw CustomException.badRequest("artifactId가 필요합니다.");
        }
        if (r.getScope() == UploadPresignRequestDTO.Scope.SECTION &&
                (r.getWorkflowId() == null || r.getSectionId() == null)) {
            throw CustomException.badRequest("workflowId/sectionId가 필요합니다.");
        }

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
}
