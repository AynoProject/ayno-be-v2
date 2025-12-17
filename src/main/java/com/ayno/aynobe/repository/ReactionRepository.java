package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.Reaction;
import com.ayno.aynobe.entity.enums.ReactionType;
import com.ayno.aynobe.entity.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)

    Optional<Reaction> findByUser_UserIdAndTargetTypeAndTargetIdAndReactionType(
            Long userId, TargetType targetType, Long targetId, ReactionType reactionType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reaction r where r.user.userId = :userId and r.targetType = :targetType and r.targetId = :targetId and r.reactionType = :reactionType")
    void deleteByUserAndTargetAndType(@Param("userId") Long userId,
                                      @Param("targetType") TargetType targetType,
                                      @Param("targetId") Long targetId,
                                      @Param("reactionType") ReactionType reactionType);

    void deleteByTargetIdAndTargetType(Long targetId, TargetType targetType);
}
