package com.ayno.aynobe.entity;

// reaction/Reaction.java
import com.ayno.aynobe.entity.enums.ReactionType;
import com.ayno.aynobe.entity.enums.TargetType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reaction",
        uniqueConstraints = @UniqueConstraint(name = "uq_reaction_active",
                columnNames = {"userId","targetType","targetId","reactionType"}),
        indexes = {
                @Index(name = "idx_reaction_target", columnList = "targetType, targetId, reactionType"),
                @Index(name = "idx_reaction_user",   columnList = "userId, reactionType")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaction extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "targetId", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "targetType", nullable = false, length = 20)
    private TargetType targetType; // WORKFLOW / ARTIFACT

    @Enumerated(EnumType.STRING)
    @Column(name = "reactionType", nullable = false, length = 20)
    private ReactionType reactionType; // LIKE (MVP)
}

