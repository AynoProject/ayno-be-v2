package com.ayno.aynobe.entity;

import com.ayno.aynobe.dto.artifact.ArtifactCreateRequestDTO;
import com.ayno.aynobe.dto.artifact.ArtifactUpdateRequestDTO;
import com.ayno.aynobe.entity.enums.MediaType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "artifact_media",
        indexes = {
                @Index(name = "idx_media_artifact", columnList = "artifactId"),
                @Index(name = "idx_media_artifact_order", columnList = "artifactId, sortOrder")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_artifact_media_basekey", columnNames = {"artifactId", "baseKey"})
        }
)
public class ArtifactMedia extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifactId", nullable = false)
    private Artifact artifact;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType mediaType;

    @NotBlank
    @Size(max = 512)
    @Column(nullable = false, length = 512)
    private String baseKey;

    /** 정렬 순서 (NOT NULL) */
    @Column(nullable = false)
    private int sortOrder;

    /** 커버 여부 (NOT NULL) */
    @Column(nullable = false)
    private boolean isCover;

    /** 캡션만 NULL 허용 */
    @Size(max = 256)
    @Column(length = 256)
    private String caption;

    /* ======= 도메인 메서드 ======= */

    public void assignArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public void markAsCover() { this.isCover = true; }
    public void unmarkCover() { this.isCover = false; }
    public void changeSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void changeCaption(String caption) { this.caption = caption; }

    /* ======= 팩토리 ======= */
    public static ArtifactMedia from(ArtifactCreateRequestDTO.MediaDTO dto) {
        return ArtifactMedia.builder()
                .mediaType(dto.getMediaType())   // NOT NULL 가정
                .baseKey(dto.getBaseKey())       // NOT NULL 가정
                .sortOrder(dto.getSortOrder())   // primitive or @NotNull 보장
                .isCover(dto.isCover())       // primitive or @NotNull 보장
                .caption(dto.getCaption())       // nullable
                .build();
    }

    public static ArtifactMedia from(ArtifactUpdateRequestDTO.MediaDTO dto) {
        return ArtifactMedia.builder()
                .mediaType(dto.getMediaType())
                .baseKey(dto.getBaseKey())
                .sortOrder(dto.getSortOrder())
                .isCover(dto.isCover())
                .caption(dto.getCaption())
                .build();
    }
}