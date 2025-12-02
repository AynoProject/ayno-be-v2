package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.log.ViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ViewLogRepository extends JpaRepository<ViewLog, Long> {
    Optional<ViewLog> findByArtifact_ArtifactIdAndUser_UserIdAndVisitDate(
            Long artifactId, Long userId, LocalDate visitDate
    );
}