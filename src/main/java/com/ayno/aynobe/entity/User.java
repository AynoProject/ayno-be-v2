package com.ayno.aynobe.entity;

import com.ayno.aynobe.entity.enums.AgeBand;
import com.ayno.aynobe.entity.enums.GenderType;
import com.ayno.aynobe.entity.enums.UsageDepthType;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_users_username", columnNames = "username")
        }
)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(length = 256)
    private String username;

    @Column(length = 256)
    private String nickname;

    @Column(length = 512)
    private String passwordHash;

    @Column(length = 512)
    private String profileImageUrl;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinkedAccount> linkedAccounts = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GenderType gender;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgeBand ageBand;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UsageDepthType aiUsageDepth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jobRoleId")
    private JobRole jobRole;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserInterest> userInterests = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workflow> workflows = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Artifact> artifacts = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reaction> reactions = new ArrayList<>();

    public void changeNickname(String nickname) {this.nickname = nickname;}
    public void changeProfileImageUrl(String profileImageUrl) {this.profileImageUrl = profileImageUrl;}
    public void changeGender(GenderType gender) { this.gender = gender; }
    public void changeAgeBand(AgeBand ageBand) { this.ageBand = ageBand; }
    public void changeAiUsageDepth(UsageDepthType depth) { this.aiUsageDepth = depth; }
    public void changeJobRole(JobRole jobRole) { this.jobRole = jobRole; }

    public void updateInterests(Set<Integer> interestIdsToRemove,
                                Collection<Interest> interestsToAdd) {
        if (!interestIdsToRemove.isEmpty()) {
            this.userInterests.removeIf(ui ->
                    interestIdsToRemove.contains(ui.getInterest().getInterestId()));
        }
        if (!interestsToAdd.isEmpty()) {
            for (Interest interest : interestsToAdd) {
                this.userInterests.add(UserInterest.interestBuilder(this, interest));
            }
        }
    }
}
