package com.app.globalgates.controller.admin;

import com.app.globalgates.auth.CustomUserDetails;
import com.app.globalgates.dto.RagIngestResponseDTO;
import com.app.globalgates.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
@Slf4j
public class AdminRagAPIController {
    private final RagService ragService;

    // 관리자 업로드는 빈 파일 요청을 초반에 잘라야 서비스 쪽 흐름이 단순해진다.
    @PostMapping("/documents")
    public ResponseEntity<?> uploadDocument(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "문서 파일이 필요합니다."));
        }

        log.info("RAG 문서 업로드 요청 - adminId: {}, fileName: {}", userDetails.getId(), file.getOriginalFilename());
        RagIngestResponseDTO responseDTO = ragService.uploadAndIngest(userDetails.getId(), file);
        return ResponseEntity.ok(responseDTO);
    }
}
