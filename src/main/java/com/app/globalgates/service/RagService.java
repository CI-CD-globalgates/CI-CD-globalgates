package com.app.globalgates.service;

import com.app.globalgates.common.enumeration.FileContentType;
import com.app.globalgates.common.enumeration.RagDocumentStatus;
import com.app.globalgates.common.enumeration.RagProcessStatus;
import com.app.globalgates.dto.FileDTO;
import com.app.globalgates.dto.RagDocumentDTO;
import com.app.globalgates.dto.RagIngestRequestDTO;
import com.app.globalgates.dto.RagIngestResponseDTO;
import com.app.globalgates.repository.FileDAO;
import com.app.globalgates.repository.RagDocumentDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    private final FileDAO fileDAO;
    private final RagDocumentDAO ragDocumentDAO;

    @Value("${ai.fastapi.base-url:http://127.0.0.1:8000}")
    private String fastApiBaseUrl;

    @Value("${ai.fastapi.internal-token:}")
    private String internalAiToken;

    @Value("${ai.rag.storage-root-dir:${user.dir}/storage/rag}")
    private String ragStorageRootDir;

    // 관리자 업로드 한 번으로 저장, 메타 적재, FastAPI ingest 호출까지 끝낸다.
    public RagIngestResponseDTO uploadAndIngest(Long uploadedBy, MultipartFile file) throws IOException {
        Path storedPath = saveFileToStorage(file);
        String documentName = resolveDocumentName(file.getOriginalFilename(), storedPath);
        Long fileId = null;
        Long documentId = null;

        try {
            FileDTO fileDTO = buildFileDTO(file, storedPath, documentName);
            fileDAO.save(fileDTO);
            fileId = fileDTO.getId();

            RagDocumentDTO ragDocumentDTO = buildRagDocumentDTO(uploadedBy, fileDTO.getId(), documentName);
            ragDocumentDAO.save(ragDocumentDTO);
            documentId = ragDocumentDTO.getId();

            ragDocumentDAO.updateRagStatus(documentId, RagProcessStatus.PROCESSING, null);
            requestFastApiIngest(storedPath.toString());
            ragDocumentDAO.updateRagStatus(documentId, RagProcessStatus.COMPLETED, null);

            RagIngestResponseDTO responseDTO = new RagIngestResponseDTO();
            responseDTO.setDocumentId(documentId);
            responseDTO.setDocumentName(documentName);
            responseDTO.setRagStatus(RagProcessStatus.COMPLETED);
            responseDTO.setMessage("RAG 문서 적재 요청이 완료되었습니다.");
            return responseDTO;
        } catch (Exception e) {
            log.error("RAG 문서 적재 실패 - uploadedBy: {}, fileId: {}, documentId: {}", uploadedBy, fileId, documentId, e);

            if (documentId != null) {
                // 적재 실패 파일은 남겨둬야 관리자 재시도나 원인 분석이 가능하다.
                ragDocumentDAO.updateRagStatus(documentId, RagProcessStatus.FAILED, buildErrorMessage(e));
            } else {
                rollbackLocalFile(storedPath);
                rollbackFileMetadata(fileId);
            }

            throw new RuntimeException("RAG 문서 적재에 실패했습니다.", e);
        }
    }

    // FastAPI 가 바로 읽을 수 있게 절대 경로 기반 로컬 저장소를 사용한다.
    private Path saveFileToStorage(MultipartFile file) throws IOException {
        Path directory = Path.of(ragStorageRootDir, getTodayPath());
        Files.createDirectories(directory);

        String extension = getExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + extension;
        Path storedPath = directory.resolve(storedFileName);

        Files.copy(file.getInputStream(), storedPath, StandardCopyOption.REPLACE_EXISTING);
        return storedPath.toAbsolutePath();
    }

    // tbl_file 는 실제 저장 위치와 원본 이름을 같이 알아야 후속 관리가 편하다.
    private FileDTO buildFileDTO(MultipartFile file, Path storedPath, String documentName) {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setOriginalName(documentName);
        fileDTO.setFileName(storedPath.getFileName().toString());
        fileDTO.setFilePath(storedPath.toString());
        fileDTO.setFileSize(file.getSize());
        fileDTO.setContentType(resolveContentType(file.getContentType()));
        return fileDTO;
    }

    // RAG 상태 row 는 파일 row 와 분리해서 적재 성공 여부만 독립적으로 추적한다.
    private RagDocumentDTO buildRagDocumentDTO(Long uploadedBy, Long fileId, String originalFileName) {
        RagDocumentDTO ragDocumentDTO = new RagDocumentDTO();
        ragDocumentDTO.setFileId(fileId);
        ragDocumentDTO.setDocumentName(originalFileName);
        ragDocumentDTO.setDocumentStatus(RagDocumentStatus.ACTIVE);
        ragDocumentDTO.setRagStatus(RagProcessStatus.PENDING);
        ragDocumentDTO.setUploadedBy(uploadedBy);
        return ragDocumentDTO;
    }

    // 현재 FastAPI 계약은 filePath 하나만 받으므로 Spring 도 그 모양에 맞춰 보낸다.
    private void requestFastApiIngest(String filePath) {
        RagIngestRequestDTO requestDTO = new RagIngestRequestDTO();
        requestDTO.setFilePath(filePath);

        WebClient.RequestBodySpec request = WebClient.create(fastApiBaseUrl)
                .post()
                .uri("/api/rag/ingest")
                .contentType(MediaType.APPLICATION_JSON);

        if (internalAiToken != null && !internalAiToken.isBlank()) {
            request.header("X-Internal-AI-Token", internalAiToken);
        }

        request.bodyValue(requestDTO)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // 파일 메타 저장 전 단계에서 실패한 경우에는 고아 파일을 바로 치운다.
    private void rollbackLocalFile(Path storedPath) {
        try {
            Files.deleteIfExists(storedPath);
        } catch (IOException ioException) {
            log.error("RAG 고아 파일 삭제 실패 - path: {}", storedPath, ioException);
        }
    }

    // tbl_rag_document row 가 생기기 전에 실패한 경우에는 파일 메타도 같이 지운다.
    private void rollbackFileMetadata(Long fileId) {
        if (fileId == null) {
            return;
        }

        try {
            fileDAO.delete(fileId);
        } catch (Exception e) {
            log.error("RAG 파일 메타 삭제 실패 - fileId: {}", fileId, e);
        }
    }

    // 실패 메시지는 너무 길어지지 않게 한 줄 정도만 잘라서 남긴다.
    private String buildErrorMessage(Exception e) {
        if (e instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode() + " " + responseException.getResponseBodyAsString();
        }

        return e.getMessage();
    }

    private String resolveDocumentName(String originalFileName, Path storedPath) {
        if (originalFileName != null && !originalFileName.isBlank()) {
            return originalFileName;
        }

        return storedPath.getFileName().toString();
    }

    private FileContentType resolveContentType(String mimeType) {
        if (mimeType == null) return FileContentType.ETC;
        if (mimeType.contains("pdf") || mimeType.contains("document") || mimeType.contains("text/")) {
            return FileContentType.DOCUMENT;
        }
        return FileContentType.ETC;
    }

    private String getTodayPath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    private String getExtension(String originalFileName) {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }

        return originalFileName.substring(originalFileName.lastIndexOf("."));
    }
}
