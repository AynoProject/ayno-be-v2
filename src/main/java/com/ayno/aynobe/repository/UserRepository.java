package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByNickname(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:startAt IS NULL OR u.createdAt >= :startAt) AND " +
            "(:endAt IS NULL OR u.createdAt <= :endAt) AND " +
            "(" +
            "   (:userId IS NOT NULL AND u.userId = :userId) OR " +
            "   (:keyword IS NOT NULL AND (" +
            "       u.nickname LIKE CONCAT('%', :keyword, '%')" +
            "   )) OR " +
            "   (:userId IS NULL AND :keyword IS NULL)" +        // 검색어 없으면 전체 조회
            ")")
    Page<User> searchUsers(
            @Param("status") UserStatus status,
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            Pageable pageable);
}
