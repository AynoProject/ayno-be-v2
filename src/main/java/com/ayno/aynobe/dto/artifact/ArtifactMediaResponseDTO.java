package com.ayno.aynobe.dto.artifact;

import com.ayno.aynobe.entity.ArtifactMedia;
import com.ayno.aynobe.entity.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ArtifactMediaResponse")
public class ArtifactMediaResponseDTO {
    private Long mediaId;
    private MediaType mediaType;
    private String baseKey;

    public static ArtifactMediaResponseDTO from(ArtifactMedia m) {
        return ArtifactMediaResponseDTO.builder()
                .mediaId(m.getMediaId())
                .mediaType(m.getMediaType())
                .baseKey(m.getBaseKey())
                .build();
    }
}
