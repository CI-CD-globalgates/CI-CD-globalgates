package com.app.globalgates.service;

import com.app.globalgates.common.enumeration.RagDocumentStatus;
import com.app.globalgates.common.enumeration.RagProcessStatus;
import com.app.globalgates.dto.FileDTO;
import com.app.globalgates.dto.RagDocumentDTO;
import com.app.globalgates.dto.RagIngestResponseDTO;
import com.app.globalgates.repository.FileDAO;
import com.app.globalgates.repository.RagDocumentDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private FileDAO fileDAO;

    @Mock
    private RagDocumentDAO ragDocumentDAO;

    @InjectMocks
    private RagService ragService;

    @TempDir
    Path tempDir;

    // 정상 흐름에서는 파일 저장, 메타 저장, FastAPI 호출, 상태 완료까지 한 번에 이어져야 한다.
    @Test
    void uploadAndIngest_savesFileAndMarksCompleted() throws Exception {
        HttpServer server = createServer(200, "{\"message\":\"문서 적재가 완료되었습니다.\"}");
        server.start();

        try {
            ReflectionTestUtils.setField(ragService, "ragStorageRootDir", tempDir.toString());
            ReflectionTestUtils.setField(ragService, "fastApiBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(ragService, "internalAiToken", "");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "sample.pdf",
                    "application/pdf",
                    "pdf-content".getBytes()
            );

            doAnswer(invocation -> {
                FileDTO fileDTO = invocation.getArgument(0);
                fileDTO.setId(11L);
                return null;
            }).when(fileDAO).save(any(FileDTO.class));

            doAnswer(invocation -> {
                RagDocumentDTO ragDocumentDTO = invocation.getArgument(0);
                ragDocumentDTO.setId(21L);
                return null;
            }).when(ragDocumentDAO).save(any(RagDocumentDTO.class));

            doNothing().when(ragDocumentDAO).updateRagStatus(eq(21L), any(RagProcessStatus.class), isNull());

            RagIngestResponseDTO response = ragService.uploadAndIngest(7L, file);

            ArgumentCaptor<FileDTO> fileCaptor = ArgumentCaptor.forClass(FileDTO.class);
            verify(fileDAO).save(fileCaptor.capture());

            ArgumentCaptor<RagDocumentDTO> ragCaptor = ArgumentCaptor.forClass(RagDocumentDTO.class);
            verify(ragDocumentDAO).save(ragCaptor.capture());

            assertEquals(21L, response.getDocumentId());
            assertEquals(RagProcessStatus.COMPLETED, response.getRagStatus());
            assertEquals("sample.pdf", response.getDocumentName());
            assertEquals(RagDocumentStatus.ACTIVE, ragCaptor.getValue().getDocumentStatus());
            assertTrue(Files.exists(Path.of(fileCaptor.getValue().getFilePath())));

            verify(ragDocumentDAO).updateRagStatus(21L, RagProcessStatus.PROCESSING, null);
            verify(ragDocumentDAO).updateRagStatus(21L, RagProcessStatus.COMPLETED, null);
        } finally {
            server.stop(0);
        }
    }

    // FastAPI 적재 실패는 숨기지 말고 failed 상태와 에러 메시지를 남겨야 한다.
    @Test
    void uploadAndIngest_marksFailedWhenFastApiReturnsError() throws Exception {
        HttpServer server = createServer(500, "{\"detail\":\"ingest failed\"}");
        server.start();

        try {
            ReflectionTestUtils.setField(ragService, "ragStorageRootDir", tempDir.toString());
            ReflectionTestUtils.setField(ragService, "fastApiBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(ragService, "internalAiToken", "");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "broken.pdf",
                    "application/pdf",
                    "pdf-content".getBytes()
            );

            doAnswer(invocation -> {
                FileDTO fileDTO = invocation.getArgument(0);
                fileDTO.setId(12L);
                return null;
            }).when(fileDAO).save(any(FileDTO.class));

            doAnswer(invocation -> {
                RagDocumentDTO ragDocumentDTO = invocation.getArgument(0);
                ragDocumentDTO.setId(22L);
                return null;
            }).when(ragDocumentDAO).save(any(RagDocumentDTO.class));

            RuntimeException exception = assertThrows(RuntimeException.class, () -> ragService.uploadAndIngest(9L, file));

            assertNotNull(exception.getMessage());
            verify(ragDocumentDAO).updateRagStatus(22L, RagProcessStatus.PROCESSING, null);
            verify(ragDocumentDAO).updateRagStatus(eq(22L), eq(RagProcessStatus.FAILED), any(String.class));
        } finally {
            server.stop(0);
        }
    }

    // 원본 파일명이 비어도 DB not-null 제약에 걸리지 않도록 저장 파일명으로 대체해야 한다.
    @Test
    void uploadAndIngest_fallsBackToStoredFileNameWhenOriginalFileNameMissing() throws Exception {
        HttpServer server = createServer(200, "{\"message\":\"문서 적재가 완료되었습니다.\"}");
        server.start();

        try {
            ReflectionTestUtils.setField(ragService, "ragStorageRootDir", tempDir.toString());
            ReflectionTestUtils.setField(ragService, "fastApiBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(ragService, "internalAiToken", "");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    null,
                    "application/pdf",
                    "pdf-content".getBytes()
            );

            doAnswer(invocation -> {
                FileDTO fileDTO = invocation.getArgument(0);
                fileDTO.setId(13L);
                return null;
            }).when(fileDAO).save(any(FileDTO.class));

            doAnswer(invocation -> {
                RagDocumentDTO ragDocumentDTO = invocation.getArgument(0);
                ragDocumentDTO.setId(23L);
                return null;
            }).when(ragDocumentDAO).save(any(RagDocumentDTO.class));

            RagIngestResponseDTO response = ragService.uploadAndIngest(5L, file);

            ArgumentCaptor<FileDTO> fileCaptor = ArgumentCaptor.forClass(FileDTO.class);
            verify(fileDAO).save(fileCaptor.capture());

            ArgumentCaptor<RagDocumentDTO> ragCaptor = ArgumentCaptor.forClass(RagDocumentDTO.class);
            verify(ragDocumentDAO).save(ragCaptor.capture());

            String storedFileName = Path.of(fileCaptor.getValue().getFilePath()).getFileName().toString();

            assertEquals(storedFileName, ragCaptor.getValue().getDocumentName());
            assertEquals(storedFileName, response.getDocumentName());
        } finally {
            server.stop(0);
        }
    }

    private HttpServer createServer(int statusCode, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/rag/ingest", exchange -> writeResponse(exchange, statusCode, body));
        return server;
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, body.getBytes().length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body.getBytes());
        }
    }
}
