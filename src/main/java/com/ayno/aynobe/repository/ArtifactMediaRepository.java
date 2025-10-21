package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.ArtifactMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtifactMediaRepository extends JpaRepository<ArtifactMedia, Long> {
    List<ArtifactMedia> findByArtifact_ArtifactIdOrderBySortOrderAscMediaIdAsc(Long artifactId);
}