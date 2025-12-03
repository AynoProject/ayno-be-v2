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
            "(:keyword IS NULL OR u.username LIKE %:keyword% OR u.nickname LIKE %:keyword%) AND " +
            "(:startAt IS NULL OR u.createdAt >= :startAt) AND " +
            "(:endAt IS NULL OR u.createdAt <= :endAt)")
    Page<User> searchUsers(
            @Param("status") UserStatus status,
            @Param("keyword") String keyword,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            Pageable pageable);
}
