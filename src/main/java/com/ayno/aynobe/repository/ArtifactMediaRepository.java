package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.ArtifactMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ArtifactMediaRepository extends JpaRepository<ArtifactMedia, Long> {
    List<ArtifactMedia> findByArtifact_ArtifactIdOrderBySortOrderAscMediaIdAsc(Long artifactId);

    @Query("SELECT am.baseKey FROM ArtifactMedia am WHERE am.baseKey IN :baseKeys")
    Set<String> findBaseKeysIn(@Param("baseKeys") List<String> baseKeys);
}