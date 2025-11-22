package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.user.*;
import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.Interest;
import com.ayno.aynobe.entity.JobRole;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.repository.ArtifactRepository;
import com.ayno.aynobe.repository.InterestRepository;
import com.ayno.aynobe.repository.JobRoleRepository;
import com.ayno.aynobe.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JobRoleRepository jobRoleRepository;
    private final InterestRepository interestRepository;
    private final ArtifactRepository artifactRepository;

    @Transactional
    public OnboardingResponseDTO getMyOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        return OnboardingResponseDTO.from(user);
    }

    @Transactional
    public OnboardingResponseDTO upsertOnboarding(Long userId, OnboardingUpsertRequestDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (req.getGender() != null) {user.changeGender(req.getGender());}
        if (req.getAgeBand() != null) {user.changeAgeBand(req.getAgeBand());}
        if (req.getUsageDepth() != null) {user.changeAiUsageDepth(req.getUsageDepth());}

        // jobRole
        if (req.getJobRoleId() != null) {
            JobRole role = jobRoleRepository.findById(req.getJobRoleId())
                    .orElseThrow(() -> CustomException.notFound("유효하지 않은 직무(JobRole)입니다."));
            user.changeJobRole(role);
        }

        if (req.getInterestIds() != null) {
            if (req.getInterestIds().size() > 3) {
                throw CustomException.badRequest("관심분야는 최대 3개까지 선택할 수 있습니다.");
            }

            // 현재 ID (LAZY 컬렉션 1쿼리로 로드)
            Set<Integer> currentIds = user.getUserInterests().stream()
                    .map(ui -> ui.getInterest().getInterestId())
                    .collect(java.util.stream.Collectors.toSet());

            // 원하는 ID
            Set<Integer> desiredIds = new HashSet<>(req.getInterestIds());

            // 제거/추가 계산
            Set<Integer> idsToRemove = new HashSet<>(currentIds);
            idsToRemove.removeAll(desiredIds);      // 현재 - 원하는

            Set<Integer> idsToAdd = new HashSet<>(desiredIds);
            idsToAdd.removeAll(currentIds);         // 원하는 - 현재

            // 추가할 것만 DB 조회
            List<Interest> interestsToAdd = Collections.emptyList();
            if (!idsToAdd.isEmpty()) {
                interestsToAdd = interestRepository.findAllById(idsToAdd);
                if (interestsToAdd.size() != idsToAdd.size()) {
                    throw CustomException.badRequest("유효하지 않은 관심요소 ID가 포함되어 있습니다.");
                }
            }

            // 도메인으로 적용 (orphanRemoval로 삭제 반영)
            user.updateInterests(idsToRemove, interestsToAdd);
        }

        return OnboardingResponseDTO.from(user);
    }

    @Transactional
    public ProfileResponseDTO getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));
        return ProfileResponseDTO.from(user);
    }

    @Transactional
    public ProfileResponseDTO updateProfile(Long userId, ProfileUpdateRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다."));

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw CustomException.conflict("이미 사용 중인 닉네임입니다.");
            }
            user.changeNickname(request.getNickname());
        }

        if (request.getGender() != null) user.changeGender(request.getGender());
        if (request.getAgeBand() != null) user.changeAgeBand(request.getAgeBand());
        if (request.getAiUsageDepth() != null) user.changeAiUsageDepth(request.getAiUsageDepth());

        if (request.getJobRoleId() != null) {
            JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
                    .orElseThrow(() -> CustomException.notFound("존재하지 않는 직무입니다."));
            user.changeJobRole(jobRole);
        }

        // 3. 관심사 변경 (팀원님의 Diffing 로직 채용)
        if (request.getInterestIds() != null) {
            if (request.getInterestIds().size() > 3) {
                throw CustomException.badRequest("관심분야는 최대 3개까지 선택할 수 있습니다.");
            }

            // 3-1. 현재 가지고 있는 ID 목록 추출
            Set<Integer> currentIds = user.getUserInterests().stream()
                    .map(ui -> ui.getInterest().getInterestId())
                    .collect(Collectors.toSet());

            // 3-2. 요청받은 ID 목록
            Set<Integer> desiredIds = new HashSet<>(request.getInterestIds());

            // 3-3. 삭제할 ID 계산 (현재 - 요청)
            Set<Integer> idsToRemove = new HashSet<>(currentIds);
            idsToRemove.removeAll(desiredIds);

            // 3-4. 추가할 ID 계산 (요청 - 현재)
            Set<Integer> idsToAdd = new HashSet<>(desiredIds);
            idsToAdd.removeAll(currentIds);

            // 3-5. 추가할 엔티티 조회 (필요한 것만 DB 조회)
            List<Interest> interestsToAdd = Collections.emptyList();
            if (!idsToAdd.isEmpty()) {
                interestsToAdd = interestRepository.findAllById(idsToAdd);
                // 요청한 ID가 DB에 실제 존재하는지 검증
                if (interestsToAdd.size() != idsToAdd.size()) {
                    throw CustomException.badRequest("유효하지 않은 관심요소 ID가 포함되어 있습니다.");
                }
            }

            // 3-6. 엔티티에게 업데이트 위임 (도메인 메서드 호출)
            user.updateInterests(idsToRemove, interestsToAdd);
        }

        return ProfileResponseDTO.from(user);
    }

    @Transactional
    public PageResponseDTO<MyArtifactListItemResponseDTO> getMyArtifact(Long userId, VisibilityType visibility, Pageable pageable) {
        Page<Artifact> artifactPage = artifactRepository.findAllMyArtifacts(
                userId,
                visibility,
                pageable
        );

        List<MyArtifactListItemResponseDTO> content = artifactPage.getContent().stream()
                .map(artifact -> MyArtifactListItemResponseDTO.builder()
                        .artifactId(artifact.getArtifactId())
                        .artifactTitle(artifact.getArtifactTitle())
                        .aiUsagePercent(artifact.getAiUsagePercent())
                        .viewCount(artifact.getViewCount())
                        .likeCount(artifact.getLikeCount())
                        .visibility(artifact.getVisibility())
                        .slug(artifact.getSlug())
                        .build())
                .toList();

        return PageResponseDTO.<MyArtifactListItemResponseDTO>builder()
                .content(content)
                .page(artifactPage.getNumber())
                .size(artifactPage.getSize())
                .totalElements(artifactPage.getTotalElements())
                .totalPages(artifactPage.getTotalPages())
                .hasNext(artifactPage.hasNext())
                .build();
    }

    @Transactional
    public PageResponseDTO<MyArtifactListItemResponseDTO> getLikedArtifacts(Long userId, Pageable pageable) {
        Page<Artifact> artifactPage = artifactRepository.findLikedArtifacts(
                userId,
                pageable
        );

        List<MyArtifactListItemResponseDTO> content = artifactPage.getContent().stream()
                .map(artifact -> MyArtifactListItemResponseDTO.builder()
                        .artifactId(artifact.getArtifactId())
                        .artifactTitle(artifact.getArtifactTitle())
                        .aiUsagePercent(artifact.getAiUsagePercent())
                        .viewCount(artifact.getViewCount())
                        .likeCount(artifact.getLikeCount())
                        .visibility(artifact.getVisibility())
                        .slug(artifact.getSlug())
                        .build())
                .toList();

        // 3. 반환
        return PageResponseDTO.<MyArtifactListItemResponseDTO>builder()
                .content(content)
                .page(artifactPage.getNumber())
                .size(artifactPage.getSize())
                .totalElements(artifactPage.getTotalElements())
                .totalPages(artifactPage.getTotalPages())
                .hasNext(artifactPage.hasNext())
                .build();
    }

}
