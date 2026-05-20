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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    private final FileDAO fileDAO;
    private final RagDocumentDAO ragDocumentDAO;
    private final S3Service s3Service;

    @Value("${ai.fastapi.base-url:http://127.0.0.1:8000}")
    private String fastApiBaseUrl;

    @Value("${ai.fastapi.internal-token:}")
    private String internalAiToken;

    // 관리자 업로드 한 번으로 저장, 메타 적재, FastAPI ingest 호출까지 끝낸다.
    public RagIngestResponseDTO uploadAndIngest(Long uploadedBy, MultipartFile file) throws IOException {
        String s3Key = null;
        Long fileId = null;
        Long documentId = null;

        try {
            s3Key = s3Service.uploadFile(file, "rag/" + getTodayPath());
            String documentName = resolveDocumentName(file.getOriginalFilename(), s3Key);

            FileDTO fileDTO = buildFileDTO(file, s3Key, documentName);
            fileDAO.save(fileDTO);
            fileId = fileDTO.getId();

            RagDocumentDTO ragDocumentDTO = buildRagDocumentDTO(uploadedBy, fileDTO.getId(), documentName);
            ragDocumentDAO.save(ragDocumentDTO);
            documentId = ragDocumentDTO.getId();

            ragDocumentDAO.updateRagStatus(documentId, RagProcessStatus.PROCESSING, null);
            requestFastApiIngest(s3Key);
            ragDocumentDAO.updateRagStatus(documentId, RagProcessStatus.COMPLETED, null);

            RagIngestResponseDTO responseDTO = new RagIngestResponseDTO();
            responseDTO.setDocumentId(documentId);
            responseDTO.setDocumentName(documentName);
            responseDTO.setRagStatus(RagProcessStatus.COMPLETED);
            responseDTO.setMessage("RAG 문서 적재 요청이 완료되었습니다.");
            return responseDTO;
        } catch (Exception e) {
            log.error("RAG 문서 적재 실패 - uploadedBy: {}, fileId: {}, documentId: {}, s3Key: {}",
                    uploadedBy, fileId, documentId, s3Key, e);

            if (documentId != null) {
                // 적재 실패 파일은 남겨둬야 관리자 재시도나 원인 분석이 가능하다.
                ragDocumentDAO.updateRagStatus(documentId, RagProcessStatus.FAILED, buildErrorMessage(e));
            } else {
                rollbackFileMetadata(fileId);
                rollbackS3File(s3Key);
            }

            throw new RuntimeException("RAG 문서 적재에 실패했습니다.", e);
        }
    }

    // tbl_file 는 실제 저장 위치와 원본 이름을 같이 알아야 후속 관리가 편하다.
    private FileDTO buildFileDTO(MultipartFile file, String s3Key, String documentName) {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setOriginalName(documentName);
        fileDTO.setFileName(s3Key.substring(s3Key.lastIndexOf("/") + 1));
        fileDTO.setFilePath(s3Key);
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

    // Spring 과 FastAPI 는 서로 다른 런타임이므로 로컬 경로가 아니라 S3 key 를 계약으로 사용한다.
    private void requestFastApiIngest(String s3Key) {
        RagIngestRequestDTO requestDTO = new RagIngestRequestDTO();
        requestDTO.setS3Key(s3Key);

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

    private void rollbackS3File(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }

        try {
            s3Service.deleteFile(s3Key);
        } catch (Exception e) {
            log.error("RAG S3 파일 삭제 실패 - s3Key: {}", s3Key, e);
        }
    }

    // 실패 메시지는 너무 길어지지 않게 한 줄 정도만 잘라서 남긴다.
    private String buildErrorMessage(Exception e) {
        if (e instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode() + " " + responseException.getResponseBodyAsString();
        }

        return e.getMessage();
    }

    private String resolveDocumentName(String originalFileName, String s3Key) {
        if (originalFileName != null && !originalFileName.isBlank()) {
            return originalFileName;
        }

        return s3Key.substring(s3Key.lastIndexOf("/") + 1);
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
}
