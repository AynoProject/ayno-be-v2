package com.ayno.aynobe.service;

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
public class UploadService {

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

    @Transactional(readOnly = true)
    public void deletePrivateObject(User actor, UploadDeleteRequestDTO req) {
        // --- 권한 검증 ---
        switch (req.getScope()) {
            case ARTIFACT -> {
                if (req.getArtifactId() == null) throw CustomException.badRequest("artifactId 필요");
                var art = artifactRepository.findById(req.getArtifactId())
                        .orElseThrow(() -> CustomException.notFound("존재하지 않는 결과물입니다."));
                if (!art.getUser().getUserId().equals(actor.getUserId()))
                    throw CustomException.forbidden("본인 결과물의 파일만 삭제할 수 있습니다.");
            }
            case SECTION -> {
                if (req.getWorkflowId() == null || req.getSectionId() == null)
                    throw CustomException.badRequest("workflowId/sectionId 필요");
                var wf = workflowRepository.findById(req.getWorkflowId())
                        .orElseThrow(() -> CustomException.notFound("존재하지 않는 워크플로우입니다."));
                if (!wf.getUser().getUserId().equals(actor.getUserId()))
                    throw CustomException.forbidden("본인 워크플로우의 파일만 삭제할 수 있습니다.");
                boolean ok = stepSectionRepository
                        .existsBySectionIdAndWorkflowStep_Workflow_WorkflowId(req.getSectionId(), req.getWorkflowId());
                if (!ok) throw CustomException.badRequest("섹션 정보가 유효하지 않습니다.");
            }
        }

        // --- 삭제 대상 키 구성 (private 디렉터리) ---
        String baseKey = req.getBaseKey();
        String ext = extOf(baseKey); // ".png" 등 확장자 추출
        String privateDir = toPrivateDirPrefix(baseKey); // ".../prod/private/.../media/<uuid>/"

        List<String> names = new ArrayList<>();
        names.add("original." + ext);
        if (IMG.contains(ext)) {
            names.addAll(List.of("w320.jpg","w800.jpg","w1600.jpg")); // 혹시 있으면 같이 정리(멱등)
        }

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

    /** baseKey → private 전체키 → 디렉터리 prefix(".../")로 변환 */
    private String toPrivateDirPrefix(String baseKey) {
        String keyWithOriginal = pathGen.toPrivateKey(baseKey);      // ".../private/.../original.ext"
        return keyWithOriginal.replaceAll("/original\\.[^.]+$", "/"); // ".../private/.../"
    }

    /** baseKey의 확장자 (소문자, 점 제외) */
    private static String extOf(String baseKey) {
        int dot = baseKey.lastIndexOf('.');
        return (dot > -1) ? baseKey.substring(dot + 1).toLowerCase() : "";
    }
}
