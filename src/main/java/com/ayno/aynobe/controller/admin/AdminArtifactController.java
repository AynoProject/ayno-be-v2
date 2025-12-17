package com.ayno.aynobe.controller.admin;

import com.ayno.aynobe.service.admin.AdminArtifactService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminArtifact", description = "관리자 결과물 관리 API")
@RestController
@RequestMapping("/api/admin/artifacts")
@RequiredArgsConstructor
public class AdminArtifactController {
    private final AdminArtifactService adminArtifactService;

}
