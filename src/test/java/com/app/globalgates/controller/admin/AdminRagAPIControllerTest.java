package com.app.globalgates.controller.admin;

import com.app.globalgates.auth.CustomUserDetails;
import com.app.globalgates.dto.MemberDTO;
import com.app.globalgates.dto.RagIngestResponseDTO;
import com.app.globalgates.service.RagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRagAPIControllerTest {

    @Mock
    private RagService ragService;

    @InjectMocks
    private AdminRagAPIController adminRagAPIController;

    // 파일 누락은 컨트롤러에서 바로 잘라야 서비스까지 불필요하게 내려가지 않는다.
    @Test
    void uploadDocument_returnsBadRequestWhenFileIsMissing() throws Exception {
        MemberDTO admin = new MemberDTO();
        admin.setId(1L);

        CustomUserDetails userDetails = new CustomUserDetails(admin, "admin@example.com");

        ResponseEntity<?> response = adminRagAPIController.uploadDocument(null, userDetails);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("message", "문서 파일이 필요합니다."), response.getBody());
    }

    // 정상 요청은 업로드 파일과 관리자 id를 서비스로 그대로 넘겨야 한다.
    @Test
    void uploadDocument_delegatesToService() throws Exception {
        MemberDTO admin = new MemberDTO();
        admin.setId(3L);

        CustomUserDetails userDetails = new CustomUserDetails(admin, "admin@example.com");
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "pdf".getBytes());

        RagIngestResponseDTO responseDTO = new RagIngestResponseDTO();
        responseDTO.setDocumentId(21L);
        responseDTO.setDocumentName("sample.pdf");
        responseDTO.setMessage("RAG 문서 적재 요청이 완료되었습니다.");

        when(ragService.uploadAndIngest(3L, file)).thenReturn(responseDTO);

        ResponseEntity<?> response = adminRagAPIController.uploadDocument(file, userDetails);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(ragService).uploadAndIngest(3L, file);
    }
}
