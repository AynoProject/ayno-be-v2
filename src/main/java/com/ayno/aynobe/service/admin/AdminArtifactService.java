package com.ayno.aynobe.service.admin;

import com.ayno.aynobe.dto.admin.AdminArtifactResponseDTO;
import com.ayno.aynobe.dto.admin.AdminUserResponseDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.repository.ArtifactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminArtifactService {
    private final ArtifactRepository artifactRepository;

    public PageResponseDTO<AdminArtifactResponseDTO> getArtifacts(
            VisibilityType status,
            String keyword,
            Pageable pageable
    ) {
        // 숫자가 들어오면 ID로 인식
        Long artifactId = null;
        if (keyword != null && keyword.matches("^[0-9]+$")) {
            artifactId = Long.parseLong(keyword);
        }

        Page<Artifact> artifactPage = artifactRepository.searchArtifactsForAdmin(status, artifactId, keyword, pageable);

        List<AdminArtifactResponseDTO> content = artifactPage.getContent().stream()
                .map(AdminArtifactResponseDTO::from)
                .toList();

        return PageResponseDTO.<AdminArtifactResponseDTO>builder()
                .content(content)
                .page(artifactPage.getNumber())
                .size(artifactPage.getSize())
                .totalElements(artifactPage.getTotalElements())
                .totalPages(artifactPage.getTotalPages())
                .hasNext(artifactPage.hasNext())
                .build();
    }
}
