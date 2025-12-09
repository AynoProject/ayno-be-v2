package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.enums.FlowType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    @EntityGraph(attributePaths = {"user"})
    Page<Artifact> findByVisibility(VisibilityType visibility, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Artifact> findByVisibilityAndCategory(VisibilityType visibility, FlowType category, Pageable pageable);

    boolean existsBySlug(String slug);
    boolean existsBySlugAndArtifactIdNot(String slug, Long artifactId);

    @Query("SELECT a FROM Artifact a " +
            "JOIN FETCH a.user u " +
            "LEFT JOIN FETCH a.medias m " +
            "WHERE a.artifactId = :artifactId")
    Optional<Artifact> findDetailById(@Param("artifactId") Long artifactId);

    @Query("SELECT a FROM Artifact a " +
            "WHERE a.user.userId = :userId " +
            "AND (:visibility IS NULL OR a.visibility = :visibility) " +
            "ORDER BY a.createdAt DESC")
    Page<Artifact> findAllMyArtifacts(
            @Param("userId") Long userId,
            @Param("visibility") VisibilityType visibilityType,
            Pageable pageable
    );

    @Query("SELECT a FROM Artifact a " +
            "JOIN Reaction r ON a.artifactId = r.targetId " +
            "WHERE r.user.userId = :userId " +
            "AND r.targetType = 'ARTIFACT' " +
            "AND r.reactionType = 'LIKE' " +
            "AND a.visibility = 'PUBLIC' " +
            "ORDER BY r.createdAt DESC")
    Page<Artifact> findLikedArtifacts(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Artifact a " +
            "set a.likeCount = a.likeCount + :delta " +
            "where a.artifactId = :artifactId")
    int updateLikeCount(
            @Param("artifactId") Long artifactId,
            @Param("delta") long delta);

    @Modifying(clearAutomatically = false, flushAutomatically = false)
    @Query("update Artifact a " +
            "set a.viewCount = a.viewCount + 1 " +
            "where a.artifactId = :artifactId")
    int increaseViewCount(Long artifactId);

    @Query("SELECT a FROM Artifact a " +
            "JOIN FETCH a.user u " +  // N+1 방지: 작성자 정보 한 번에 로딩
            "WHERE a.visibility = 'PUBLIC' " +
            "AND (:category IS NULL OR a.category = :category) " +
            "AND (:keyword IS NULL OR a.artifactTitle LIKE %:keyword%)") // 제목 검색
    Page<Artifact> searchPublic(
            @Param("category") FlowType category,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}