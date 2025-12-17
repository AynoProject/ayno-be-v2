package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.Report;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.ReportStatus;
import com.ayno.aynobe.entity.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:targetType IS NULL OR r.targetType = :targetType) AND " +
            "(:startAt IS NULL OR r.createdAt >= :startAt) AND " +
            "(:endAt IS NULL OR r.createdAt <= :endAt)")
    Page<Report> searchReports(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            Pageable pageable);

    boolean existsByReporterAndTargetIdAndTargetType(User reporter, Long targetId, ReportTargetType targetType);

    void deleteByTargetIdAndTargetType(Long targetId, ReportTargetType targetType);
}
