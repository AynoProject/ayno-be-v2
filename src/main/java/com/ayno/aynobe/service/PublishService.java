package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.asset.ArtifactPublishResponseDTO;
import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.ArtifactMedia;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.repository.ArtifactMediaRepository;
import com.ayno.aynobe.repository.ArtifactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublishService {
    private final ArtifactRepository artifactRepository;
    private final ArtifactMediaRepository artifactMediaRepository;
    private final MediaVariantService variantService;

    @Transactional
    public ArtifactPublishResponseDTO publishArtifact(User user, long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> CustomException.notFound("결과물 없음"));
        if (!artifact.getUser().getUserId().equals(user.getUserId()))
            throw CustomException.forbidden("본인 결과물만 발행 가능");

        List<ArtifactMedia> medias = artifactMediaRepository.findByArtifact_ArtifactIdOrderBySortOrderAscMediaIdAsc(artifactId);
        for (var m : medias) {
            variantService.publishOne(m.getBaseKey());
        }

        artifact.publish();

        return ArtifactPublishResponseDTO.builder()
                .artifactId(artifactId)
                .visibility(artifact.getVisibility())
                .publishedMediaCount(medias.size())
                .build();
    }

    @Transactional
    public ArtifactPublishResponseDTO unpublishArtifact(User actor, long artifactId) {
        var art = artifactRepository.findById(artifactId)
                .orElseThrow(() -> CustomException.notFound("결과물 없음"));
        if (!art.getUser().getUserId().equals(actor.getUserId()))
            throw CustomException.forbidden("본인 결과물만 수정 불가");

        var medias = artifactMediaRepository
                .findByArtifact_ArtifactIdOrderBySortOrderAscMediaIdAsc(artifactId);

        // 선택: public 사본 정리(삭제) — 운영 정책에 맞게 on/off
        medias.forEach(m -> variantService.deletePublicCopies(m.getBaseKey()));

        art.unpublish();

        return ArtifactPublishResponseDTO.builder()
                .artifactId(artifactId)
                .visibility(art.getVisibility())
                .publishedMediaCount(medias.size())
                .build();
    }
}
