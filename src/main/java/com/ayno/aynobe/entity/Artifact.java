package com.ayno.aynobe.entity;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.artifact.ArtifactCreateRequestDTO;
import com.ayno.aynobe.dto.artifact.ArtifactUpdateRequestDTO;
import com.ayno.aynobe.entity.enums.FlowType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "artifact",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_artifact_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_artifact_user", columnList = "user_id"),
                @Index(name = "idx_artifact_workflow", columnList = "workflow_id"),
                @Index(name = "idx_artifact_visibility_created", columnList = "visibility, created_at")
        }
)
public class Artifact extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long artifactId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "workflowId") // Artifact 테이블이 FK를 가짐
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FlowType category;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String artifactTitle;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPremium = false;

    @Min(0) @Max(100)
    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer aiUsagePercent;

    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long likeCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisibilityType visibility;

    @NotBlank
    @Column(nullable = false, length = 256)
    private String slug;

    @Builder.Default
    @OneToMany(mappedBy = "artifact", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("mediaId ASC")
    private List<ArtifactMedia> medias = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (visibility == null) visibility = VisibilityType.PRIVATE;
        if (aiUsagePercent == null) aiUsagePercent = 0;
        if (isPremium == null) isPremium = false;
        if (viewCount == null) viewCount = 0L;
        if (likeCount == null) likeCount = 0L;
    }

    public static Artifact create(User owner, Workflow workflow, ArtifactCreateRequestDTO dto) {
        return Artifact.builder()
                .user(owner)
                .workflow(workflow)                  // nullable
                .category(dto.getCategory())
                .artifactTitle(dto.getArtifactTitle())
                .isPremium(Boolean.TRUE.equals(dto.getIsPremium()))
                .aiUsagePercent(dto.getAiUsagePercent())
                .visibility(dto.getVisibility())
                .slug(dto.getSlug())
                .build();
    }

    /** 미디어 일괄 추가 (DTO → Entity 변환 + 역참조 세팅) */
    public void addMediasFrom(List<ArtifactCreateRequestDTO.MediaDTO> mediaDtos) {
        if (mediaDtos == null || mediaDtos.isEmpty()) return;

        // 1) baseKey 중복 제거(등장 순서 유지)
        Map<String, ArtifactCreateRequestDTO.MediaDTO> unique = new LinkedHashMap<>();
        for (var m : mediaDtos) {
            unique.putIfAbsent(m.getBaseKey(), m);
        }
        List<ArtifactCreateRequestDTO.MediaDTO> dedup = new ArrayList<>(unique.values());

        // 2) 커버 개수 검증
        long coverCnt = dedup.stream().filter(ArtifactCreateRequestDTO.MediaDTO::isCover).count();
        if (coverCnt > 1) {
            throw CustomException.badRequest("커버 이미지는 1개까지만 허용됩니다.");
        }

        // 3) 엔티티 변환 + 역참조 세팅
        List<ArtifactMedia> list = new ArrayList<>(dedup.size());
        for (var m : dedup) {
            ArtifactMedia em = ArtifactMedia.from(m);
            em.assignArtifact(this);
            list.add(em);
        }

        // 4) 커버가 없으면 첫 번째를 커버로 지정
        if (coverCnt == 0 && !list.isEmpty()) {
            list.get(0).markAsCover();
        }

        // 5) 추가
        this.medias.addAll(list);
    }

    public void applyHeader(ArtifactUpdateRequestDTO dto) {
        // 필드 갱신(커스텀 세터 역할)
        this.category = dto.getCategory();
        this.artifactTitle = dto.getArtifactTitle();
        this.visibility = dto.getVisibility();
        this.aiUsagePercent = dto.getAiUsagePercent();
        this.isPremium = Boolean.TRUE.equals(dto.getIsPremium());
        this.slug = dto.getSlug();
    }

    public void assignWorkflow(Workflow workflow) { // nullable 허용
        this.workflow = workflow;
    }

    /** MVP: 미디어 전체 교체(기존 orphanRemoval=true 가정) */
    public void replaceMediasFrom(List<ArtifactUpdateRequestDTO.MediaDTO> mediaDtos) {
        this.medias.clear();                           // orphanRemoval=true 가정 → 기존 행 제거
        if (mediaDtos == null || mediaDtos.isEmpty()) return;

        // 1) baseKey 중복 제거 (등장 순서 유지)
        Map<String, ArtifactUpdateRequestDTO.MediaDTO> unique = new LinkedHashMap<>();
        for (var m : mediaDtos) unique.putIfAbsent(m.getBaseKey(), m);
        List<ArtifactUpdateRequestDTO.MediaDTO> dedup = new ArrayList<>(unique.values());

        // 2) 커버 개수 검증
        long coverCnt = dedup.stream().filter(ArtifactUpdateRequestDTO.MediaDTO::isCover).count();
        if (coverCnt > 1) throw new IllegalArgumentException("커버 이미지는 1개까지만 허용합니다.");

        // 3) 엔티티 변환 + 역참조
        List<ArtifactMedia> list = new ArrayList<>(dedup.size());
        for (var m : dedup) {
            ArtifactMedia em = ArtifactMedia.from(m); // baseKey / sortOrder / isCover / caption 모두 NOT NULL(캡션만 null)
            em.assignArtifact(this);
            list.add(em);
        }

        // 4) 커버가 없다면 첫 번째를 커버
        if (coverCnt == 0 && !list.isEmpty()) list.get(0).markAsCover();

        this.medias.addAll(list);
    }

    public void publish() {
        if (this.visibility == VisibilityType.PUBLIC) return; // 이미 공개면 무시
        this.visibility = VisibilityType.PUBLIC;
    }

    public void unpublish() {
        if (this.visibility == VisibilityType.PRIVATE) return;
        this.visibility = VisibilityType.PRIVATE;
    }
}
