package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.artifact.*;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.ArtifactMedia;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.Workflow;
import com.ayno.aynobe.entity.enums.FlowType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.repository.ArtifactRepository;
import com.ayno.aynobe.repository.StepSectionRepository;
import com.ayno.aynobe.repository.WorkflowRepository;
import com.ayno.aynobe.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtifactService {
    private final ArtifactRepository artifactRepository;
    private final WorkflowRepository workflowRepository;
    private final StepSectionRepository stepSectionRepository;
    private final S3Service s3Service;

    public PageResponseDTO<ArtifactListItemResponseDTO> listPublic(
            FlowType category, int page, int size, String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));

        Page<Artifact> result = (category == null)
                ? artifactRepository.findByVisibility(VisibilityType.PUBLIC, pageable)
                : artifactRepository.findByVisibilityAndCategory(VisibilityType.PUBLIC, category, pageable);

        return PageResponseDTO.<ArtifactListItemResponseDTO>builder()
                .content(result.getContent().stream().map(ArtifactListItemResponseDTO::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .build();
    }

    @Transactional
    public ArtifactDetailResponseDTO getDetail(Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> CustomException.notFound("Artifact with id " + artifactId + " not found"));
        // LAZY 로딩 허용: 트랜잭션 내에서 medias 접근
        artifact.getMedias().size();
        return ArtifactDetailResponseDTO.from(artifact);
    }

    @Transactional
    public ArtifactCreateResponseDTO create(User owner, ArtifactCreateRequestDTO requestDto) {

        validateForCreate(requestDto);

        // 워크플로우 연결: 존재 확인만 하고 참조 프록시로 세팅
        Workflow workflow = null;
        if (requestDto.getWorkflowId() != null) {
            Long wfId = requestDto.getWorkflowId();
            if (!workflowRepository.existsById(wfId)) {
                throw CustomException.notFound("존재하지 않는 Workflow ID: " + wfId);
            }
            workflow = workflowRepository.getReferenceById(wfId); // SELECT 없이 FK 참조
        }

        // 루트 생성 (도메인 팩토리: DTO 그대로)
        Artifact artifact = Artifact.create(owner, workflow, requestDto);

        // 미디어 추가 (도메인 메서드)
        artifact.addMediasFrom(requestDto.getMedias());

        try {
            Artifact saved = artifactRepository.saveAndFlush(artifact);
            return new ArtifactCreateResponseDTO(saved.getArtifactId());
        } catch (DataIntegrityViolationException e) {
            throw CustomException.badRequest("중복된 이미지가 포함되어 저장에 실패했습니다. : " + e);
        }
    }

    private void validateForCreate(ArtifactCreateRequestDTO dto) {
        if (artifactRepository.existsBySlug(dto.getSlug())) {
            throw CustomException.duplicate("중복된 슬러그입니다: " + dto.getSlug());
        }
    }

    @Transactional
    public ArtifactUpdateResponseDTO update(User actor, Long artifactId, ArtifactUpdateRequestDTO dto) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 결과물입니다."));

        if (!artifact.getUser().getUserId().equals(actor.getUserId())) {
            throw CustomException.forbidden("본인이 등록한 결과물만 수정할 수 있습니다.");
        }

        // 검증: slug 유니크(자기 자신 제외)
        if (artifactRepository.existsBySlugAndArtifactIdNot(dto.getSlug(), artifactId)) {
            throw CustomException.duplicate("중복된 슬러그입니다: " + dto.getSlug());
        }

        // ★ 2. S3 삭제 로직을 위해 *기존* 워크플로우와 *새* 워크플로우 ID를 미리 가져옵니다.
        Workflow oldWorkflow = artifact.getWorkflow(); // 현재 DB에 연결된 워크플로우
        Long newWorkflowId = dto.getWorkflowId();   // DTO로 전달된 새 워크플로우 ID (null일 수 있음)

        // ★ 3.'고아'가 될 워크플로우가 있는지 감지 (연결 해제 또는 교체 시)
        if (oldWorkflow != null && (newWorkflowId == null || !oldWorkflow.getWorkflowId().equals(newWorkflowId))) {
            // 기존 워크플로우가 있었는데, (1) 새 ID가 null이거나 (2) 새 ID가 기존 ID와 다르면
            // -> oldWorkflow는 '고아'가 되어 DB에서 삭제될 예정입니다. (by orphanRemoval)

            // DB 레코드가 삭제되기 전에, S3에 저장된 섹션 파일들을 *먼저* 수동으로 삭제합니다.
            // (N+1 방지를 위해 baseKey 목록을 한번에 조회)
            List<String> sectionBaseKeys = stepSectionRepository.findAllBaseKeysByWorkflowId(oldWorkflow.getWorkflowId());
            for (String baseKey : sectionBaseKeys) {
                if (baseKey != null && !baseKey.isBlank()) {
                    s3Service.deleteS3MediaSet(baseKey); // (public/private 모두 삭제하는 헬퍼)
                }
            }
        }

        // ★ 5. (수정) 변수명을 newWorkflow로 명확히 하고, DTO의 ID로 새 프록시를 로드합니다.
        Workflow newWorkflow = null;
        if (newWorkflowId != null) {
            Long wfId = newWorkflowId; // 변수명 일관성
            if (!workflowRepository.existsById(wfId)) {
                throw CustomException.notFound("존재하지 않는 Workflow ID: " + wfId);
            }
            newWorkflow = workflowRepository.getReferenceById(wfId);
        }

        // 도메인 위임(더티체킹)
        artifact.applyHeader(dto);
        artifact.assignWorkflow(newWorkflow);            // nullable 허용
        artifact.replaceMediasFrom(dto.getMedias());  // 전체 교체

        try {
            Artifact saved = artifactRepository.saveAndFlush(artifact);
            return ArtifactUpdateResponseDTO.builder()
                    .artifactId(saved.getArtifactId())
                    .build();
        } catch (DataIntegrityViolationException e) {
            // (artifactId, baseKey) 유니크나 coverKey 유니크 위반 등
            throw CustomException.badRequest("미디어 중복 또는 제약 조건 위반으로 수정에 실패했습니다. : " + e);
        }
    }

    @Transactional
    public ArtifactDeleteResponseDTO delete(User user, Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 결과물입니다."));

        if (!artifact.getUser().getUserId().equals(user.getUserId())) {
            throw CustomException.forbidden("본인이 등록한 결과물만 삭제할 수 있습니다.");
        }

        // 1. S3 물리적 파일 삭제
        Workflow workflow = artifact.getWorkflow();
        if (workflow != null) {
            Long workflowId = workflow.getWorkflowId();

            // 워크플로우 ID로 모든 섹션의 baseKey를 한번에 조회 (N+1 방지)
            List<String> sectionBaseKeys = stepSectionRepository.findAllBaseKeysByWorkflowId(workflowId);

            for (String baseKey : sectionBaseKeys) {
                s3Service.deleteS3MediaSet(baseKey);
            }
        }

        for (ArtifactMedia media : artifact.getMedias()) {
            s3Service.deleteS3MediaSet(media.getBaseKey());
        }

        // 2. DB 삭제
        artifactRepository.delete(artifact);

        return ArtifactDeleteResponseDTO.builder()
                .artifactId(artifactId)
                .build();
    }

    public PageResponseDTO<ArtifactListItemResponseDTO> searchPublicArtifacts(
            FlowType category,
            String keyword,
            String sort,
            Pageable pageable
    ) {
        // 1. 정렬 조건 처리 (Pageable 객체 재생성)
        // 프론트에서 "latest"라고 보내면 "createAt"으로, "popular"면 "likeCount"로 변환
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                resolveSort(sort)
        );

        // 2. 검색 실행
        Page<Artifact> result = artifactRepository.searchPublic(category, keyword, sortedPageable);

        // 3. DTO 변환 (우리가 아까 만든 Light DTO 사용)
        return PageResponseDTO.<ArtifactListItemResponseDTO>builder()
                .content(result.getContent().stream()
                        .map(ArtifactListItemResponseDTO::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .build();
    }

    private Sort resolveSort(String sort) {
        String field = "createdAt";
        Sort.Direction dir = Sort.Direction.DESC;

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);

            if (parts.length >= 1 && !parts[0].isBlank()) {
                String candidate = parts[0].trim();
                if ("createdAt".equals(candidate) || "likeCount".equals(candidate) || "viewCount".equals(candidate)) {
                    field = candidate;
                }
            }
            if (parts.length == 2 && "asc".equalsIgnoreCase(parts[1].trim())) {
                dir = Sort.Direction.ASC;
            }
        }
        return Sort.by(dir, field);
    }
}
